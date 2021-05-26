//
//  PolyvSceneRNModule.h
//  PolyvRNCloudClassDemo
//
//  Created by Sakya on 2021/4/20.
//  Copyright © 2021 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, PolyvCloudClassErrorCode) {
    PolyvCloudClassError_Success = 0,
    PolyvCloudClassError_NoAppId = -1,
    PolyvCloudClassError_NoAppSecret = -2,
    PolyvCloudClassError_NoViewerId = -3,
    PolyvCloudClassError_NoUserId = -4,
    PolyvCloudClassError_NoChannelId = -5,
    PolyvCloudClassError_NoVodId = -6,
    PolyvCloudClassError_LoginFailed = -10,
    ///服务器或者其它未知错误
    PolyvCloudClassError_Other = -99,
};

@interface PolyvSceneRNModule : NSObject<RCTBridgeModule>

@end

NS_ASSUME_NONNULL_END
