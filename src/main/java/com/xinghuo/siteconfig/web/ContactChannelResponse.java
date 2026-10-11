package com.xinghuo.siteconfig.web;

public record ContactChannelResponse(
        String label,
        String wechatId,
        String qrImage,
        String note,
        String jumpUrl) {
}
