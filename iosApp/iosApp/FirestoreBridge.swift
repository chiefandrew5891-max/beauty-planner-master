import Foundation
import FirebaseFirestore

final class FirestoreBridge {

    private static func userRoot(_ userId: String) -> DocumentReference {
        Firestore.firestore().collection("users").document(userId)
    }

    static func pullAll(
        userId: String,
        completion: @escaping ([String: String]?, String?) -> Void
    ) {
        let root = userRoot(userId)

        root.collection("appointments").getDocuments { snapshot, error in
            if let error = error {
                completion(nil, error.localizedDescription)
                return
            }

            let docs = snapshot?.documents ?? []
            let payloads: [String] = docs.compactMap { doc in
                doc.data()["payload"] as? String
            }

            let appointmentsJson: String
            do {
                let data = try JSONSerialization.data(withJSONObject: payloads.compactMap { payload -> Any? in
                    guard let payloadData = payload.data(using: .utf8),
                          let jsonObject = try? JSONSerialization.jsonObject(with: payloadData) else {
                        return nil
                    }
                    return jsonObject
                })
                appointmentsJson = String(data: data, encoding: .utf8) ?? "[]"
            } catch {
                completion(nil, error.localizedDescription)
                return
            }

            root.collection("private")
                .document("app")
                .collection("meta")
                .document("settings")
                .getDocument { settingsSnapshot, settingsError in
                    if let settingsError = settingsError {
                        completion(nil, settingsError.localizedDescription)
                        return
                    }

                    var settingsJson = ""

                    if let settingsData = settingsSnapshot?.data(),
                       let payload = settingsData["payload"] as? String {
                        settingsJson = payload
                    }

                    completion(
                        [
                            "appointments": appointmentsJson,
                            "settings": settingsJson
                        ],
                        nil
                    )
                }
        }
    }

    static func pushAppointments(
        userId: String,
        appointments: [[String: String]],
        completion: @escaping ([String: String]?, String?) -> Void
    ) {
        let db = Firestore.firestore()
        let collection = userRoot(userId).collection("appointments")

        if appointments.isEmpty {
            completion([:], nil)
            return
        }

        let chunks = stride(from: 0, to: appointments.count, by: 350).map {
            Array(appointments[$0..<min($0 + 350, appointments.count)])
        }

        func commitChunk(_ index: Int) {
            if index >= chunks.count {
                completion([:], nil)
                return
            }

            let batch = db.batch()

            for appointment in chunks[index] {
                guard let id = appointment["id"], !id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
                    continue
                }

                let updatedAtMillis = Int64(appointment["updatedAtMillis"] ?? "") ?? 0
                let isDeleted = (appointment["isDeleted"] ?? "").lowercased() == "true"

                let docData: [String: Any] = [
                    "id": appointment["id"] ?? "",
                    "dateString": appointment["dateString"] ?? "",
                    "time": appointment["time"] ?? "",
                    "updatedAtMillis": updatedAtMillis,
                    "isDeleted": isDeleted,
                    "paymentStatus": appointment["paymentStatus"] ?? "",
                    "payload": appointment["payload"] ?? ""
                ]

                batch.setData(docData, forDocument: collection.document(id))
            }

            batch.commit { error in
                if let error = error {
                    completion(nil, error.localizedDescription)
                } else {
                    commitChunk(index + 1)
                }
            }
        }

        commitChunk(0)
    }

    static func pushSettings(
        userId: String,
        settings: [String: String],
        completion: @escaping ([String: String]?, String?) -> Void
    ) {
        let updatedAtMillis = Int64(settings["updatedAtMillis"] ?? "") ?? 0
        let payload = settings["payload"] ?? ""

        userRoot(userId)
            .collection("private")
            .document("app")
            .collection("meta")
            .document("settings")
            .setData([
                "updatedAtMillis": updatedAtMillis,
                "payload": payload
            ]) { error in
                if let error = error {
                    completion(nil, error.localizedDescription)
                } else {
                    completion([:], nil)
                }
            }
    }
}