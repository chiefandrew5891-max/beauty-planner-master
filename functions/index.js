/**
 * Beauty Planner – Firebase Cloud Functions
 *
 * This is the canonical backend source for all callable Firebase Functions used
 * by the Beauty Planner app.  Deploy this file to update the entire backend:
 *
 *   cd functions && npm install
 *   firebase deploy --only functions
 *
 * Functions defined here:
 *   bootstrapUser        – create or retrieve a user account and start trial
 *   syncIdentity         – update identity fields for an existing user
 *   verifySubscription   – record an in-app purchase and unlock premium access
 *   getAccessStatus      – return the current access tier for a user
 *   checkAppUpdate       – return update availability for a given app version
 *   processRtdn          – handle Google Play Real-Time Developer Notifications
 *   syncMasterProfile    – write master profile data to masters/{userId}
 *
 * Firestore collections:
 *   users/{userId}   – identity, access tier, subscription state
 *   masters/{userId} – master profile for cross-app sharing (Client Booker)
 *   config/appUpdate – optional per-platform update configuration document
 */

'use strict';

const functions = require('firebase-functions');
const admin     = require('firebase-admin');

if (admin.apps.length === 0) {
    admin.initializeApp();
}

const db = admin.firestore();

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const TRIAL_DURATION_MS = 14 * 24 * 60 * 60 * 1000; // 14 days

// ---------------------------------------------------------------------------
// bootstrapUser
// ---------------------------------------------------------------------------

/**
 * Callable function: bootstrapUser
 *
 * Called once per installation when the user first signs in.  Creates a new
 * user document in users/{docId} if one does not already exist for the given
 * firebaseUid, starts the free trial, and returns an AccessStatusResponse.
 *
 * If a document already exists for this firebaseUid the function updates
 * identity/install fields and returns the current access status.
 *
 * Input fields:
 *   installId    – string  (per-device installation identifier)
 *   firebaseUid  – string  (Firebase Auth UID)
 *   platform     – string  ("ANDROID" or "IOS")
 *   authProvider – string  (e.g. "google", "anonymous")
 *   email        – string
 *   displayName  – string
 */
exports.bootstrapUser = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'Authentication required.');
    }

    const firebaseUid  = safeString(data.firebaseUid) || context.auth.uid;
    const installId    = safeString(data.installId);
    const platform     = safeString(data.platform).toUpperCase() || 'ANDROID';
    const authProvider = safeString(data.authProvider);
    const email        = safeString(data.email);
    const displayName  = safeString(data.displayName);
    const now          = Date.now();

    // Find existing user document by firebaseUid.
    const snapshot = await db.collection('users')
        .where('firebaseUid', '==', firebaseUid)
        .limit(1)
        .get();

    if (!snapshot.empty) {
        // User already exists – update identity/install fields and return
        // the current access status.
        const doc  = snapshot.docs[0];
        const data = doc.data();
        await doc.ref.update({
            installId,
            platform,
            authProvider,
            email,
            displayName,
            updatedAt: now,
        });
        return buildAccessResponse(doc.id, data);
    }

    // First-time registration – create user document and start trial.
    const trialEndsAtMillis = now + TRIAL_DURATION_MS;
    const newUser = {
        firebaseUid,
        installId,
        platform,
        authProvider,
        email,
        displayName,
        tier: 'TRIAL',
        trialStartedAtMillis: now,
        trialEndsAtMillis,
        subscriptionState: 'NONE',
        premiumProductId: '',
        subscriptionExpiryMillis: 0,
        subscriptionAutoRenewing: false,
        subscriptionOrderId: '',
        createdAt: now,
        updatedAt: now,
    };

    const newRef = await db.collection('users').add(newUser);
    return buildAccessResponse(newRef.id, newUser);
});

// ---------------------------------------------------------------------------
// syncIdentity
// ---------------------------------------------------------------------------

/**
 * Callable function: syncIdentity
 *
 * Updates the identity fields (email, displayName, authProvider) on an
 * existing user document.  Called after sign-in to keep identity data fresh.
 *
 * Input fields:
 *   firebaseUid  – string
 *   email        – string
 *   displayName  – string
 *   authProvider – string
 */
exports.syncIdentity = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'Authentication required.');
    }

    const firebaseUid  = safeString(data.firebaseUid) || context.auth.uid;
    const email        = safeString(data.email);
    const displayName  = safeString(data.displayName);
    const authProvider = safeString(data.authProvider);
    const now          = Date.now();

    const snapshot = await db.collection('users')
        .where('firebaseUid', '==', firebaseUid)
        .limit(1)
        .get();

    if (snapshot.empty) {
        throw new functions.https.HttpsError('not-found', 'User not found.');
    }

    const doc     = snapshot.docs[0];
    const current = doc.data();
    await doc.ref.update({ email, displayName, authProvider, updatedAt: now });
    return buildAccessResponse(doc.id, { ...current, email, displayName, authProvider });
});

// ---------------------------------------------------------------------------
// verifySubscription
// ---------------------------------------------------------------------------

/**
 * Callable function: verifySubscription
 *
 * Records a completed in-app purchase and elevates the user's tier to PREMIUM.
 * The server-side receipt/token validation step should be added here once the
 * corresponding Google Play / App Store server keys are configured.
 *
 * Input fields:
 *   userId        – string  (Firestore users document ID)
 *   productId     – string
 *   purchaseToken – string
 *   platform      – string  ("PLAY" or "APPSTORE")
 *   transactionId – string
 */
exports.verifySubscription = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'Authentication required.');
    }

    const userId        = safeString(data.userId);
    const productId     = safeString(data.productId);
    const purchaseToken = safeString(data.purchaseToken);
    const platform      = safeString(data.platform).toUpperCase() || 'PLAY';
    const transactionId = safeString(data.transactionId);
    const now           = Date.now();

    if (!userId) {
        throw new functions.https.HttpsError('invalid-argument', 'userId is required.');
    }

    const userRef = db.doc(`users/${userId}`);
    const userDoc = await userRef.get();

    if (!userDoc.exists) {
        throw new functions.https.HttpsError('not-found', 'User document not found.');
    }

    // Verify caller owns this user document.
    if (userDoc.data().firebaseUid !== context.auth.uid) {
        throw new functions.https.HttpsError('permission-denied', 'Access denied.');
    }

    // Determine subscription expiry (default: 1 year from now).
    const subscriptionExpiryMillis = now + 365 * 24 * 60 * 60 * 1000;

    const update = {
        tier: 'PREMIUM',
        subscriptionState: 'ACTIVE',
        premiumProductId: productId,
        subscriptionExpiryMillis,
        subscriptionAutoRenewing: true,
        subscriptionOrderId: transactionId || purchaseToken.substring(0, 64),
        updatedAt: now,
    };

    await userRef.update(update);

    const updatedData = { ...userDoc.data(), ...update };
    return buildAccessResponse(userId, updatedData);
});

// ---------------------------------------------------------------------------
// getAccessStatus
// ---------------------------------------------------------------------------

/**
 * Callable function: getAccessStatus
 *
 * Returns the current access tier and subscription information for a user.
 *
 * Input fields:
 *   userId – string  (Firestore users document ID)
 */
exports.getAccessStatus = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'Authentication required.');
    }

    const userId = safeString(data.userId);
    if (!userId) {
        throw new functions.https.HttpsError('invalid-argument', 'userId is required.');
    }

    const userDoc = await db.doc(`users/${userId}`).get();
    if (!userDoc.exists) {
        throw new functions.https.HttpsError('not-found', 'User document not found.');
    }

    // Verify caller owns this user document.
    if (userDoc.data().firebaseUid !== context.auth.uid) {
        throw new functions.https.HttpsError('permission-denied', 'Access denied.');
    }

    return buildAccessResponse(userId, userDoc.data());
});

// ---------------------------------------------------------------------------
// checkAppUpdate
// ---------------------------------------------------------------------------

/**
 * Callable function: checkAppUpdate
 *
 * Returns update availability metadata for the requesting client version.
 * Configuration is read from the Firestore document config/appUpdate.
 * If that document does not exist the function returns a no-update response.
 *
 * Input fields:
 *   platform    – string  ("ANDROID" or "IOS")
 *   versionName – string  (e.g. "2.1.0")
 *   buildNumber – string  (e.g. "210")
 *
 * Response fields:
 *   updateAvailable  – "true" | "false"
 *   forceUpdate      – "true" | "false"
 *   latestVersion    – string
 *   updateUrl        – string
 */
exports.checkAppUpdate = functions.https.onCall(async (data, _context) => {
    const platform    = safeString(data.platform).toUpperCase();
    const versionName = safeString(data.versionName);
    const buildNumber = safeString(data.buildNumber);

    const configDoc = await db.doc('config/appUpdate').get();
    if (!configDoc.exists) {
        return {
            updateAvailable: 'false',
            forceUpdate: 'false',
            latestVersion: versionName,
            updateUrl: '',
        };
    }

    const config        = configDoc.data();
    const platformKey   = platform === 'IOS' ? 'ios' : 'android';
    const platformCfg   = (config && config[platformKey]) || {};
    const latestVersion = safeString(platformCfg.latestVersion);
    const minVersion    = safeString(platformCfg.minVersion);
    const updateUrl     = safeString(platformCfg.updateUrl);

    const updateAvailable = latestVersion && latestVersion !== versionName;
    const forceUpdate     = minVersion    && isVersionLower(versionName, minVersion);

    return {
        updateAvailable: updateAvailable ? 'true' : 'false',
        forceUpdate:     forceUpdate     ? 'true' : 'false',
        latestVersion:   latestVersion || versionName,
        updateUrl,
    };
});

// ---------------------------------------------------------------------------
// processRtdn  (Google Play Real-Time Developer Notifications)
// ---------------------------------------------------------------------------

/**
 * Pub/Sub function: processRtdn
 *
 * Receives Real-Time Developer Notifications from Google Play and updates the
 * corresponding user's subscription state in Firestore.
 *
 * Topic is typically: projects/{projectId}/topics/play-rtdn
 * Configure the subscription in Google Play Console → Monetization setup.
 */
exports.processRtdn = functions.pubsub
    .topic('play-rtdn')
    .onPublish(async (message) => {
        let payload;
        try {
            payload = message.json;
        } catch (e) {
            functions.logger.error('processRtdn: failed to parse message', e);
            return;
        }

        const subscriptionNotification = payload && payload.subscriptionNotification;
        if (!subscriptionNotification) {
            functions.logger.info('processRtdn: no subscriptionNotification in message, skipping.');
            return;
        }

        const { purchaseToken, notificationType, subscriptionId } = subscriptionNotification;
        if (!purchaseToken) {
            functions.logger.warn('processRtdn: missing purchaseToken, skipping.');
            return;
        }

        // Map notification type to subscription state.
        // https://developer.android.com/google/play/billing/rtdn-reference
        const stateMap = {
            1:  'RECOVERED',
            2:  'RENEWED',
            3:  'CANCELED',
            4:  'PURCHASED',
            5:  'ON_HOLD',
            6:  'IN_GRACE_PERIOD',
            7:  'RESTARTED',
            12: 'REVOKED',
            13: 'EXPIRED',
        };
        const subscriptionState = stateMap[notificationType] || 'UNKNOWN';
        const isActive = ['RECOVERED', 'RENEWED', 'PURCHASED', 'RESTARTED', 'IN_GRACE_PERIOD']
            .includes(subscriptionState);
        const now = Date.now();

        // Find the user whose last purchase token matches.
        const snapshot = await db.collection('users')
            .where('subscriptionOrderId', '>=', purchaseToken.substring(0, 64))
            .limit(1)
            .get();

        if (snapshot.empty) {
            functions.logger.warn('processRtdn: no user found for purchaseToken prefix.');
            return;
        }

        const userRef = snapshot.docs[0].ref;
        await userRef.update({
            subscriptionState,
            tier: isActive ? 'PREMIUM' : 'FREE_LIMITED',
            premiumProductId: subscriptionId || '',
            subscriptionAutoRenewing: isActive,
            updatedAt: now,
        });

        functions.logger.info(`processRtdn: updated user ${userRef.id} state=${subscriptionState}`);
    });

// ---------------------------------------------------------------------------
// syncMasterProfile
// ---------------------------------------------------------------------------

/**
 * Callable function: syncMasterProfile
 *
 * Writes master profile data supplied by the app into masters/{userId}.
 * This document is the cross-app profile shared with Beauty Planner Client
 * Booker.
 *
 * Security:
 *   The caller must be authenticated.  The provided userId is the Firestore
 *   users document ID (NOT the Firebase Auth UID).  Ownership is validated by
 *   loading users/{userId} and comparing its stored firebaseUid against the
 *   caller's context.auth.uid.
 *
 * Firestore document path: masters/{userId}
 *
 * Fields written:
 *   userId                    – string  (Firestore users doc ID)
 *   ownerName                 – string  (trimmed)
 *   searchableOwnerName       – string  (lowercase, for future search use)
 *   profileDisplayCustomName  – boolean
 *   profilePhone              – string  (trimmed)
 *   profilePhoneVisible       – boolean
 *   profileSpecialization     – string  (trimmed)
 *   searchableSpecialization  – string  (lowercase, for future search use)
 *   profileRating             – number  (clamped 0–5)
 *   profileAvatarUrl          – string
 *   profileAvatarBase64       – string  (cropped avatar; takes precedence over URL)
 *   clientInteractionsEnabled – boolean
 *   serviceTemplates          – array   (parsed from serviceTemplatesJson)
 *   createdAt                 – number  (epoch ms, set only on first creation)
 *   updatedAt                 – number  (epoch ms, updated on every sync)
 *
 * Avatar precedence rule:
 *   If profileAvatarBase64 is non-empty it is the primary avatar source.
 *   Otherwise profileAvatarUrl is used.  Both fields are stored so the client
 *   app can apply this rule itself.
 */
exports.syncMasterProfile = functions.https.onCall(async (data, context) => {
    // 1. Require authentication.
    if (!context.auth) {
        throw new functions.https.HttpsError(
            'unauthenticated',
            'Authentication is required to sync a master profile.'
        );
    }

    // 2. Validate userId.
    const userId = safeString(data.userId);
    if (!userId) {
        throw new functions.https.HttpsError(
            'invalid-argument',
            'userId is required.'
        );
    }

    // 3. Ownership validation:
    //    userId is the Firestore users document ID, not the Firebase Auth UID.
    //    Load the user document and compare its stored firebaseUid with the
    //    caller's authenticated UID.
    const userDoc = await db.doc(`users/${userId}`).get();
    if (!userDoc.exists) {
        throw new functions.https.HttpsError(
            'not-found',
            'User document not found.'
        );
    }
    if (userDoc.data().firebaseUid !== context.auth.uid) {
        throw new functions.https.HttpsError(
            'permission-denied',
            'You can only sync your own master profile.'
        );
    }

    // 4. Normalize and validate all incoming fields.
    const ownerName              = safeString(data.ownerName);
    const profileDisplayCustomName = normalizeBoolean(data.profileDisplayCustomName);
    const profilePhone           = safeString(data.profilePhone);
    const profilePhoneVisible    = normalizeBoolean(data.profilePhoneVisible);
    const profileSpecialization  = safeString(data.profileSpecialization);
    const profileRating          = normalizeRating(data.profileRating);
    const profileAvatarUrl       = safeString(data.profileAvatarUrl);
    // profileAvatarBase64 may be large – only accept strings, default to empty.
    const profileAvatarBase64    = (typeof data.profileAvatarBase64 === 'string')
        ? data.profileAvatarBase64
        : '';
    const clientInteractionsEnabled = normalizeBoolean(data.clientInteractionsEnabled);
    const serviceTemplates       = parseServiceTemplates(data.serviceTemplatesJson);

    const masterRef = db.doc(`masters/${userId}`);
    const now       = Date.now();

    const profilePayload = {
        userId,
        ownerName,
        searchableOwnerName: ownerName.toLowerCase(),
        profileDisplayCustomName,
        profilePhone,
        profilePhoneVisible,
        profileSpecialization,
        searchableSpecialization: profileSpecialization.toLowerCase(),
        profileRating,
        profileAvatarUrl,
        profileAvatarBase64,
        clientInteractionsEnabled,
        serviceTemplates,
        updatedAt: now,
    };

    // 5. Preserve createdAt on the first write; merge updates thereafter.
    const existingDoc = await masterRef.get();
    if (!existingDoc.exists) {
        profilePayload.createdAt = now;
        await masterRef.set(profilePayload);
    } else {
        await masterRef.set(profilePayload, { merge: true });
    }

    return { success: true, updatedAt: now };
});

// ---------------------------------------------------------------------------
// Helper utilities
// ---------------------------------------------------------------------------

/**
 * Build an AccessStatusResponse object from a Firestore user document.
 * The shape must match the AccessStatusResponse data class on the client.
 */
function buildAccessResponse(userId, data) {
    const now               = Date.now();
    const trialEndsAtMillis = data.trialEndsAtMillis || 0;
    const tier              = resolveTier(data.tier, trialEndsAtMillis, now);
    const isTrialActive     = tier === 'TRIAL' && trialEndsAtMillis > now;
    const trialDaysLeft     = isTrialActive
        ? Math.ceil((trialEndsAtMillis - now) / (24 * 60 * 60 * 1000))
        : 0;

    return {
        userId,
        tier,
        trialStartedAtMillis:     data.trialStartedAtMillis     || 0,
        trialEndsAtMillis,
        isTrialActive,
        hasPremium:               tier === 'PREMIUM',
        trialDaysLeft,
        subscriptionState:        data.subscriptionState        || 'NONE',
        premiumProductId:         data.premiumProductId         || '',
        subscriptionExpiryMillis: data.subscriptionExpiryMillis || 0,
        subscriptionAutoRenewing: data.subscriptionAutoRenewing || false,
        subscriptionOrderId:      data.subscriptionOrderId      || '',
    };
}

/**
 * Resolve the effective access tier taking expiry into account.
 * A TRIAL that has passed its end date is downgraded to FREE_LIMITED.
 * A PREMIUM whose subscriptionExpiryMillis has passed is also downgraded.
 */
function resolveTier(storedTier, trialEndsAtMillis, now) {
    if (storedTier === 'TRIAL') {
        return trialEndsAtMillis > now ? 'TRIAL' : 'FREE_LIMITED';
    }
    return storedTier || 'FREE_LIMITED';
}

/**
 * Coerce a value to a trimmed string.  Returns '' for null / undefined.
 */
function safeString(value) {
    if (value === null || value === undefined) return '';
    return value.toString().trim();
}

/**
 * Normalize a value that may arrive as a native boolean (Android) or as the
 * string "true"/"false" (iOS) to an actual boolean.
 */
function normalizeBoolean(value) {
    if (value === null || value === undefined) return false;
    if (typeof value === 'boolean') return value;
    return value.toString().toLowerCase() === 'true';
}

/**
 * Normalize a rating value to a float in [0, 5].
 * Falls back to the default rating (4.7) when the value is missing or invalid.
 */
function normalizeRating(value) {
    if (value === null || value === undefined || value === '') return 4.7;
    const n = parseFloat(value.toString());
    if (isNaN(n)) return 4.7;
    return Math.max(0, Math.min(5, n));
}

/**
 * Parse serviceTemplates from either a JSON string (the format sent by the
 * mobile app) or a plain array.  Returns an empty array on any parse error.
 *
 * Each valid template must be an object with at least an `id` string field.
 */
function parseServiceTemplates(json) {
    if (!json) return [];
    if (Array.isArray(json)) {
        return sanitizeServiceTemplates(json);
    }
    try {
        const parsed = JSON.parse(json.toString());
        if (!Array.isArray(parsed)) return [];
        return sanitizeServiceTemplates(parsed);
    } catch (_) {
        return [];
    }
}

/**
 * Validate and sanitize an array of service template objects, keeping only
 * entries that have a non-empty string `id` field.
 */
function sanitizeServiceTemplates(arr) {
    return arr.filter(item =>
        item !== null &&
        typeof item === 'object' &&
        typeof item.id === 'string' &&
        item.id.trim().length > 0
    );
}

/**
 * Return true if versionA is strictly lower than versionB using semver-like
 * dot-separated integer comparison (e.g. "2.0.1" < "2.1.0").
 */
function isVersionLower(versionA, versionB) {
    const partsA = versionA.split('.').map(Number);
    const partsB = versionB.split('.').map(Number);
    const len    = Math.max(partsA.length, partsB.length);
    for (let i = 0; i < len; i++) {
        const a = partsA[i] || 0;
        const b = partsB[i] || 0;
        if (a < b) return true;
        if (a > b) return false;
    }
    return false;
}
