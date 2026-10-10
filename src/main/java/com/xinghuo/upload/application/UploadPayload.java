package com.xinghuo.upload.application;

public record UploadPayload(byte[] content, String mediaType) {
}
