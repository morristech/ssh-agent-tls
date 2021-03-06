/*-
 * -\-\-
 * ssh-agent-tls
 * --
 * Copyright (C) 2016 - 2017 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

package com.spotify.sshagenttls;

import com.google.auto.value.AutoValue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;

@AutoValue
public abstract class CertKey {

  CertKey() {
    // Prevent outside instantiation
  }

  public abstract Certificate cert();

  public abstract PrivateKey key();

  static CertKey create(final Certificate cert,
                        final PrivateKey key) {
    return new AutoValue_CertKey(cert, key);
  }

  public static CertKey fromPaths(final Path certPath, final Path keyPath)
      throws IOException, GeneralSecurityException {
    // TODO (dxia) Cert might not be X.509
    final CertificateFactory cf = CertificateFactory.getInstance("X.509");

    final Certificate cert;
    try (final InputStream is = Files.newInputStream(certPath)) {
      cert = cf.generateCertificate(is);
    }

    final Object parsedPem;
    try (final BufferedReader br = Files.newBufferedReader(keyPath, Charset.defaultCharset())) {
      parsedPem = new PEMParser(br).readObject();
    }

    final PrivateKeyInfo keyInfo;
    if (parsedPem instanceof PEMKeyPair) {
      keyInfo = ((PEMKeyPair) parsedPem).getPrivateKeyInfo();
    } else if (parsedPem instanceof PrivateKeyInfo) {
      keyInfo = (PrivateKeyInfo) parsedPem;
    } else {
      throw new UnsupportedOperationException("Unable to parse X.509 certificate, received " + parsedPem.getClass());
    }

    final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyInfo.getEncoded());
    final KeyFactory kf = KeyFactory.getInstance("RSA");

    return new AutoValue_CertKey(cert, kf.generatePrivate(spec));
  }
}
