#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface FirestoreBridge : NSObject

+ (void)pullAllForUserId:(NSString *)userId
        completion:(void (^)(NSDictionary * _Nullable result, NSString * _Nullable error))completion;

+ (void)pushAppointmentsForUserId:(NSString *)userId
        appointments:(NSArray<NSDictionary *> *)appointments
        completion:(void (^)(NSString * _Nullable error))completion;

+ (void)pushSettingsForUserId:(NSString *)userId
        settings:(NSDictionary *)settings
        completion:(void (^)(NSString * _Nullable error))completion;

@end

        NS_ASSUME_NONNULL_END