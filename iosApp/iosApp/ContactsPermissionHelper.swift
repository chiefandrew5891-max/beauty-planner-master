import Foundation
import Contacts

@objc final class ContactsPermissionHelper: NSObject {

    @objc static func isPermissionGranted() -> Bool {
        let status = CNContactStore.authorizationStatus(for: .contacts)
        return status == .authorized || status == .limited
    }

    @objc static func requestPermission(
        completion: @escaping (Bool) -> Void
    ) {
        let status = CNContactStore.authorizationStatus(for: .contacts)

        if status == .authorized || status == .limited {
            DispatchQueue.main.async {
                completion(true)
            }
            return
        }

        if status == .denied || status == .restricted {
            DispatchQueue.main.async {
                completion(false)
            }
            return
        }

        let store = CNContactStore()
        store.requestAccess(for: .contacts) { granted, _ in
            DispatchQueue.main.async {
                completion(granted)
            }
        }
    }

    @objc static func findSuggestions(
        query: String,
        limit: Int,
        completion: @escaping ([[String: String]]) -> Void
    ) {
        let status = CNContactStore.authorizationStatus(for: .contacts)
        guard status == .authorized || status == .limited else {
            DispatchQueue.main.async {
                completion([])
            }
            return
        }

        let trimmedQuery = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmedQuery.count >= 2 else {
            DispatchQueue.main.async {
                completion([])
            }
            return
        }

        DispatchQueue.global(qos: .userInitiated).async {
            let lowerQuery = trimmedQuery.lowercased()
            let store = CNContactStore()

            let keys: [CNKeyDescriptor] = [
                CNContactGivenNameKey as NSString,
                CNContactFamilyNameKey as NSString,
                CNContactPhoneNumbersKey as NSString
            ]

            let request = CNContactFetchRequest(keysToFetch: keys)

            var results: [[String: String]] = []
            var seen = Set<String>()

            do {
                try store.enumerateContacts(with: request) { contact, stop in
                    if results.count >= limit {
                        stop.pointee = true
                        return
                    }

                    let fullName = [contact.givenName, contact.familyName]
                        .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
                        .filter { !$0.isEmpty }
                        .joined(separator: " ")
                        .trimmingCharacters(in: .whitespacesAndNewlines)

                    guard !fullName.isEmpty else { return }
                    guard fullName.lowercased().contains(lowerQuery) else { return }

                    guard let firstPhoneRaw = contact.phoneNumbers.first?.value.stringValue else {
                        return
                    }

                    let firstPhone = firstPhoneRaw.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard !firstPhone.isEmpty else { return }

                    let key = "\(fullName.lowercased())|\(firstPhone)"
                    guard !seen.contains(key) else { return }
                    seen.insert(key)

                    results.append([
                        "displayName": fullName,
                        "phone": firstPhone
                    ])
                }

                DispatchQueue.main.async {
                    completion(results)
                }
            } catch {
                DispatchQueue.main.async {
                    completion([])
                }
            }
        }
    }
}