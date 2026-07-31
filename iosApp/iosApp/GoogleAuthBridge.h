#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface GoogleAuthBridge : NSObject

+ (void)signInWithGoogleWithCompletion:(void (^)(NSDictionary * _Nullable result, NSString * _Nullable error))completion;
+ (void)signInAnonymouslyWithCompletion:(void (^)(NSDictionary * _Nullable result, NSString * _Nullable error))completion;
+ (NSDictionary * _Nullable)currentUser;
+ (NSString * _Nullable)signOutUser;
+ (NSString * _Nullable)prepareForNewSignIn;
+ (void)callBackend:(NSString *)name
        payload:(NSDictionary *)payload
        completion:(void (^)(NSDictionary * _Nullable result, NSString * _Nullable error))completion;

@end

        NS_ASSUME_NONNULL_END