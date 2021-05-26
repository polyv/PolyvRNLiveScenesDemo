'use strict';

import { NativeModules } from 'react-native';

const PolyvSceneRNModule = NativeModules.PolyvSceneRNModule

export const PolyvSceneModule = {
    /**
     * 设置直播账号配置信息；建议通过服务器加密存储获取，然后在本地进行解密后再设置
     * 启动应用后应当设置，设置完成后可进入多场景观看页
     * @param userId 直播后台开发账号userId
     * @param appId 直播后台开发账号appId
     * @param appSecret 直播后台开发账号appSecret
     * @returns {Promise<{code: *, message: *}|{code: number}>}
     */
    async setConfig(userId, appId, appSecret) {
        try {
            await PolyvSceneRNModule.setConfig(userId, appId, appSecret);
            return { "code": 0 }
        } catch (e) {
            var code = e.code;
            var message = e.message;
            return { code, message }
        }
    },

    async setViewerInfo(viewerId, viewerName, viewerAvatar) {
        console.log(`js sdk setViewerInfo_${viewerId}_${viewerName}`)
        try {
            await PolyvSceneRNModule.setViewerInfo(viewerId, viewerName, viewerAvatar)
            return { "code": 0 }
        } catch (e) {
            var code = e.code;
            var message = e.message;
            console.log(`js sdk setViewerInfo_result_${e.code}_${e.message}`)

            return { code, message }
        }
    },

    /**
     * 登录直播
     * @param type 观看的场景[1-云课堂； 2-直播带货]
     * @param channelId 频道号
     * @returns {Promise<{code: *, message: *}|{code: number}>}
     */
    async loginLive(handler, type, channelId) {
        console.log(`loginLive_${channelId}_${type}`)
        try {
            await PolyvSceneRNModule.loginLive(handler, type, channelId)
            return { "code": 0 }
        } catch (e) {
            var code = e.code;
            var message = e.message;
            return { code, message }
        }
    },

    /**
     * 登录回放
     * @param sceneType 观看的场景[1-云课堂； 2-直播带货]
     * @param channelId 频道号
     * @param vid 回放视频videoId
     * @param vodType 回放视频类型。[0-回放列表；1-点播列表]
     * @returns {Promise<{code: *, message: *}|{code: number}>}
     */
    async loginPlayback(handler, sceneType, channelId, vid, vodType) {
        console.log(`login_${channelId}_${sceneType}_${vid}_${vodType}`)
        try {
            await PolyvSceneRNModule.loginPlayback(handler, sceneType, channelId, vid, vodType)
            return { "code": 0 }
        } catch (e) {
            var code = e.code;
            var message = e.message;
            return { code, message }
        }

    }
}

module.export = PolyvSceneModule;
