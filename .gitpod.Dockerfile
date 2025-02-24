FROM gitpod/workspace-full

USER gitpod

# Install Android SDK
ENV ANDROID_HOME=/workspace/android-sdk
ENV PATH=${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}

RUN mkdir -p ${ANDROID_HOME} \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O android-sdk.zip \
    && unzip -q android-sdk.zip -d ${ANDROID_HOME}/cmdline-tools \
    && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm android-sdk.zip

# Accept licenses and install required packages
RUN yes | sdkmanager --licenses \
    && sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" "extras;google;m2repository" "extras;android;m2repository"

# Install Gradle
ENV GRADLE_VERSION=8.2
RUN wget -q https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -O gradle.zip \
    && unzip -q gradle.zip -d /opt \
    && rm gradle.zip
ENV GRADLE_HOME=/opt/gradle-${GRADLE_VERSION}
ENV PATH=${GRADLE_HOME}/bin:${PATH} 