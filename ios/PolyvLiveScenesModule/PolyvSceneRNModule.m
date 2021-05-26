//
//  PolyvSceneRNModule.m
//  PolyvRNCloudClassDemo
//
//  Created by Sakya on 2021/4/20.
//  Copyright © 2021 Polyv. All rights reserved.
//

#import "PolyvSceneRNModule.h"
#import <React/RCTLog.h>
#import <React/RCTUIManager.h>
#import <PolyvFoundationSDK/PLVProgressHUD.h>
#import "PLVRoomLoginClient.h"
#import "PLVRoomDataManager.h"

#import "PLVLiveSDKConfig.h"
#import "PLVLCCloudClassViewController.h"
#import "PLVECWatchRoomViewController.h"
#import "PLVBugReporter.h"

NSString * NSStringFromErrorCode(PolyvCloudClassErrorCode code) {
    switch (code) {
        case PolyvCloudClassError_Success:
            return @"成功";
        case PolyvCloudClassError_NoAppId:
            return @"AppId为空";
        case PolyvCloudClassError_NoAppSecret:
            return @"AppSecret为空";
        case PolyvCloudClassError_NoUserId:
            return @"UserId为空";
        case PolyvCloudClassError_NoChannelId:
            return @"ChannelId为空";
        case PolyvCloudClassError_NoVodId:
            return @"VodId为空";
        case PolyvCloudClassError_NoViewerId:
            return @"ViewerId为空";
        case PolyvCloudClassError_LoginFailed:
            return @"频道登录失败";
        default:
            return @"";
    }
}
///播放室类型
typedef NS_ENUM(NSInteger, PolyvRNRoomType) {
    ///云课堂直播
    PolyvRNRoomCloudClassLiveType = 1,
    ///云课堂回放
    PolyvRNRoomCloudClassPlaybackType = 2,
    ///带货直播
    PolyvRNRoomEcommerceLiveType = 3,
    ///带货回放
    PolyvRNRoomEcommercePlaybackType = 4,
};

@implementation PolyvSceneRNModule

#define OpenBugly 0

@synthesize bridge = _bridge;

RCT_EXPORT_MODULE();

/// 配置登录直播间信息
/// @param userId 用户id
/// @param appId 应用id
/// @param appSecret 应用secret
RCT_EXPORT_METHOD(
                  setConfig:(NSString *)userId
                  appId:(NSString *)appId
                  appSecret:(NSString *)appSecret
                  findEventsWithResolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                  )
{
    NSLog(@"setConfig() - %@ 、 %@ 、  %@ ", userId, appId, appSecret);
    RCTLogInfo(@"setConfig() - %@ 、 %@ 、 %@ ", userId, appId, appSecret);
    
    PolyvCloudClassErrorCode errorCode = PolyvCloudClassError_Success;
    if (!userId.length) {
        errorCode = PolyvCloudClassError_NoUserId;
    } else if (!appId.length) {
        errorCode = PolyvCloudClassError_NoAppId;
    } else if (!appSecret.length) {
        errorCode = PolyvCloudClassError_NoAppSecret;
    }
    
    if (errorCode == PolyvCloudClassError_Success) {
        /// 参数配置
        [[PLVLiveVideoConfig sharedInstance] configWithUserId:userId appId:appId appSecret:appSecret];
        resolve(@[@(PolyvCloudClassError_Success)]);
    } else {
        NSString *errorDesc = NSStringFromErrorCode(errorCode);
        NSError *error = [NSError errorWithDomain:NSURLErrorDomain code:errorCode userInfo:@{NSLocalizedDescriptionKey:errorDesc}];
        NSLog(@"%@", errorDesc);
        reject([@(errorCode) stringValue], errorDesc, error);
    }
}

/// 设置参观者用户信息
/// @param viewerId
RCT_EXPORT_METHOD(
                  setViewerInfo:(NSString *)viewerId
                  viewerName:(NSString *)viewerName
                  viewerAvatar:(NSString *)viewerAvatar
                  findEventsWithResolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                  )
{
    RCTLogInfo(@"setViewerInfo() - %@ 、 %@ 、 %@ ", viewerId, viewerName, viewerAvatar);
    if (viewerId.length > 0) { // 配置统计后台参数：用户Id、用户昵称及自定义参数
        [PLVLiveSDKConfig configViewUserWithUserId:viewerId
                                        viewerName:viewerName
                                      viewerAvatar:viewerAvatar
                                        viewerType:0];
        resolve(@[@(PolyvCloudClassError_Success)]);
    } else {
        [self responseWithErrorCode:PolyvCloudClassError_NoViewerId
                            message:nil
                           rejecter:reject];
    }
}

/// 直播登录
/// @param type 观看的场景[1-云课堂； 2-直播带货]
/// @param channelId 频道号
RCT_EXPORT_METHOD(
                  loginLive:(nonnull NSNumber *)reactTag
                  type:(NSInteger)type
                  channelId:(NSString *)channelId
                  findEventsWithResolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                  )
{
    RCTLogInfo(@"loginLive() - %ld、 %@", (long)type, channelId);
    
    RCTUIManager *uiManager = _bridge.uiManager;
    __weak typeof(self) weakSelf = self;
    dispatch_async(uiManager.methodQueue, ^{
        [uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *,UIView *> *viewRegistry) {
            PLVProgressHUD *hud = [PLVProgressHUD showHUDAddedTo:[UIApplication sharedApplication].keyWindow animated:YES];
            [hud.label setText:@"登录中..."];
            
            //判断直播类型
            if (type == 1) {
                [weakSelf loginWithChannelId:channelId vid:nil roomType:PolyvRNRoomCloudClassLiveType success:^{
                    [hud hideAnimated:YES];
                    PLVLCCloudClassViewController * cloudClassVC = [[PLVLCCloudClassViewController alloc] init];
                    cloudClassVC.modalPresentationStyle = UIModalPresentationFullScreen;
                    UIView *view = viewRegistry[reactTag];
                    UIViewController *viewController = (UIViewController *)view.reactViewController;
                    // 进入云课堂直播间
                    [viewController presentViewController:cloudClassVC animated:YES completion:nil];
                    resolve(@[@(PolyvCloudClassError_Success)]);
                    
#if OpenBugly
                    PLVRoomUser *roomUser = [PLVRoomDataManager sharedManager].roomData.roomUser;
                    [PLVBugReporter setUserIdentifier:roomUser.viewerId];
#endif
                } failure:^(NSString *errorMessage) {
                    [hud hideAnimated:YES];
                    [weakSelf responseWithErrorCode:PolyvCloudClassError_Other message:errorMessage rejecter:reject];
                }];
            } else if (type == 2) {
                [weakSelf loginWithChannelId:channelId vid:nil roomType:PolyvRNRoomEcommerceLiveType success:^{
                    [hud hideAnimated:YES];
                    UIView *view = viewRegistry[reactTag];
                    PLVECWatchRoomViewController * watchLiveVC = [[PLVECWatchRoomViewController alloc] init];
                    watchLiveVC.modalPresentationStyle = UIModalPresentationFullScreen;
                    UIViewController *viewController = (UIViewController *)view.reactViewController;
                    // 进入带货直播间
                    [viewController presentViewController:watchLiveVC animated:YES completion:nil];
                    resolve(@[@(PolyvCloudClassError_Success)]);
                    
#if OpenBugly
                    PLVRoomUser *roomUser = [PLVRoomDataManager sharedManager].roomData.roomUser;
                    [PLVBugReporter setUserIdentifier:roomUser.viewerId];
#endif
                } failure:^(NSString * _Nonnull errorMessage) {
                    [hud hideAnimated:YES];
                    [weakSelf responseWithErrorCode:PolyvCloudClassError_Other message:errorMessage rejecter:reject];
                }];
            }
        }];
        [uiManager batchDidComplete];
    });
}

/// 登录回放
/// @param sceneType 观看的场景[1-云课堂； 2-直播带货]
/// @param channelId 频道号
/// @param vid 回放视频videoId
/// @param vodType 回放视频类型。[0-回放列表；1-点播列表]
RCT_EXPORT_METHOD(
                  loginPlayback:(nonnull NSNumber *)reactTag
                  sceneType:(NSInteger)sceneType
                  channelId:(NSString *)channelId
                  vid:(NSString *)vid
                  vodType:(NSInteger)vodType
                  findEventsWithResolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                  )
{
    RCTLogInfo(@"sceneType() - %ld、 %@", (long)sceneType, channelId);
    
    RCTUIManager *uiManager = _bridge.uiManager;
    __weak typeof(self) weakSelf = self;
    dispatch_async(uiManager.methodQueue, ^{
        [uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *,UIView *> *viewRegistry) {
            PLVProgressHUD *hud = [PLVProgressHUD showHUDAddedTo:[UIApplication sharedApplication].keyWindow animated:YES];
            [hud.label setText:@"登录中..."];
            if (sceneType == 1) {
                //云课堂回放场景
                [weakSelf loginWithChannelId:channelId vid:vid roomType:PolyvRNRoomCloudClassPlaybackType success:^{
                    [hud hideAnimated:YES];
                    PLVLCCloudClassViewController * cloudClassVC = [[PLVLCCloudClassViewController alloc] init];
                    cloudClassVC.modalPresentationStyle = UIModalPresentationFullScreen;
                    UIView *view = viewRegistry[reactTag];
                    UIViewController *viewController = (UIViewController *)view.reactViewController;
                    // 进入直播间
                    [viewController presentViewController:cloudClassVC animated:YES completion:nil];
                    resolve(@[@(PolyvCloudClassError_Success)]);
                    
#if OpenBugly
                    PLVRoomUser *roomUser = [PLVRoomDataManager sharedManager].roomData.roomUser;
                    [PLVBugReporter setUserIdentifier:roomUser.viewerId];
#endif
                } failure:^(NSString * _Nonnull errorMessage) {
                    [hud hideAnimated:YES];
                    [weakSelf responseWithErrorCode:PolyvCloudClassError_Other message:errorMessage rejecter:reject];
                }];
            } else if (sceneType == 2) {
                [weakSelf loginWithChannelId:channelId vid:vid roomType:PolyvRNRoomEcommercePlaybackType success:^{
                    [hud hideAnimated:YES];
                    UIView *view = viewRegistry[reactTag];
                    PLVECWatchRoomViewController * watchLiveVC = [[PLVECWatchRoomViewController alloc] init];
                    watchLiveVC.modalPresentationStyle = UIModalPresentationFullScreen;
                    UIViewController *viewController = (UIViewController *)view.reactViewController;
                    // 进入直播间
                    [viewController presentViewController:watchLiveVC animated:YES completion:nil];
                    resolve(@[@(PolyvCloudClassError_Success)]);
                    
#if OpenBugly
                    PLVRoomUser *roomUser = [PLVRoomDataManager sharedManager].roomData.roomUser;
                    [PLVBugReporter setUserIdentifier:roomUser.viewerId];
#endif
                } failure:^(NSString * _Nonnull errorMessage) {
                    [hud hideAnimated:YES];
                    [weakSelf responseWithErrorCode:PolyvCloudClassError_Other message:errorMessage rejecter:reject];
                }];
            }
        }];
        [uiManager batchDidComplete];
    });
    
}

#pragma mark --private
- (void)loginWithChannelId:(NSString *)channelId
                       vid:(NSString * _Nullable)videoVid
                  roomType:(PolyvRNRoomType)roomType
                   success:(void(^)(void))success
                   failure:(void (^)(NSString * _Nonnull))failure
{
    if (roomType == PolyvRNRoomCloudClassLiveType ||
        roomType ==PolyvRNRoomEcommerceLiveType) {
        PLVLiveVideoConfig *videoConfig = [PLVLiveVideoConfig sharedInstance];
        PLVChannelType channelType = roomType == PolyvRNRoomCloudClassLiveType ? PLVChannelTypePPT | PLVChannelTypeAlone : PLVChannelTypeAlone;
        [PLVRoomLoginClient loginLiveRoomWithChannelType:channelType
                                               channelId:channelId
                                                  userId:videoConfig.userId
                                                   appId:videoConfig.appId
                                               appSecret:videoConfig.appSecret
                                                roomUser:^(PLVRoomUser * _Nonnull roomUser) {
            //      可在此处配置自定义的登陆用户ID、昵称、头像，不配则均使用默认值
            //      PLVRoomViewUser *viewUser = [PLVLiveSDKConfig sharedSDK].viewUser;
            //      roomUser.viewerId = viewUser.viewerId;
            //      roomUser.viewerName = viewUser.viewerName;
            //      roomUser.viewerAvatar = viewUser.viewerAvatar;
        } completion:^(PLVViewLogCustomParam * _Nonnull customParam) {
            if(success) success();
        } failure:^(NSString * _Nonnull errorMessage) {
            if(failure) failure(errorMessage);
        }];
    } else if (roomType == PolyvRNRoomCloudClassPlaybackType ||
               roomType ==PolyvRNRoomEcommercePlaybackType) {
        PLVLiveVideoConfig *videoConfig = [PLVLiveVideoConfig sharedInstance];
        PLVChannelType channelType = roomType == PolyvRNRoomCloudClassPlaybackType ? PLVChannelTypePPT | PLVChannelTypeAlone : PLVChannelTypeAlone;
        [PLVRoomLoginClient loginPlaybackRoomWithChannelType:channelType
                                                   channelId:channelId
                                                         vid:videoVid
                                                      userId:videoConfig.userId
                                                       appId:videoConfig.appId
                                                   appSecret:videoConfig.appSecret
                                                    roomUser:^(PLVRoomUser * _Nonnull roomUser) {
            
        } completion:^(PLVViewLogCustomParam * _Nonnull customParam) {
            if(success) success();
        } failure:^(NSString * _Nonnull errorMessage) {
            if(failure) failure(errorMessage);
        }];
    }
}
///错误信息数据整合
- (void)responseWithErrorCode:(PolyvCloudClassErrorCode)errorCode
                      message:(NSString *)message
                     rejecter:(RCTPromiseRejectBlock)reject {
    NSString *errorDesc = NSStringFromErrorCode(errorCode);
    if (errorCode == PolyvCloudClassError_Other) errorDesc = message;
    NSError *error = [NSError errorWithDomain:NSURLErrorDomain code:errorCode userInfo:@{NSLocalizedDescriptionKey:errorDesc}];
    reject([@(errorCode) stringValue], errorDesc, error);
}

@end
