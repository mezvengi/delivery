FROM eclipse-temurin:17-jdk-jammy

ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${ANDROID_SDK_ROOT}/build-tools/34.0.0

RUN apt-get update && apt-get install -y --no-install-recommends \
    unzip \
    zip \
    && rm -rf /var/lib/apt/lists/*

COPY cmdline-tools.zip /tmp/cmdline.zip

RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools && \
    unzip -q /tmp/cmdline.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools && \
    mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest && \
    rm /tmp/cmdline.zip

RUN yes | sdkmanager --licenses && \
    sdkmanager "platforms;android-34" "build-tools;34.0.0"

WORKDIR /app
COPY android_app /app/android_app
COPY build_apk.sh /build_apk.sh

RUN tr -d '\r' < /build_apk.sh > /build_apk_unix.sh && \
    chmod +x /build_apk_unix.sh && \
    /build_apk_unix.sh
