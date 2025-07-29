#!/bin/bash

printf "$GPG_KEY_BASE64" | base64 --decode > gpg.asc
echo ${GPG_PASSPHRASE} | gpg --batch --yes --passphrase-fd 0 --import gpg.asc
gpg -k

./mvnw \
    -s settings.xml \
    -Pcentral \
    -Dmaven.test.skip=true \
    -Dgpg.passphrase=${GPG_PASSPHRASE} \
    clean deploy -B -D skipITs