/*
 *
 * Copyright (c) 2013 - 2017 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.security.speed.p12;

import java.security.SecureRandom;

import org.xipki.security.SecurityFactory;
import org.xipki.security.util.KeyUtil;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */
// CHECKSTYLE:SKIP
public class P12DSAKeyGenLoadTest extends P12KeyGenLoadTest {
    private final int plength;
    private final int qlength;

    public P12DSAKeyGenLoadTest(final int plength, final int qlength,
            final SecurityFactory securityFactory) throws Exception {
        super("PKCS#12 DSA key generation\nplength: " + plength + "\nqlength: " + qlength,
                securityFactory);

        this.plength = plength;
        this.qlength = qlength;
    }

    @Override
    protected void generateKeypair(final SecureRandom random) throws Exception {
        KeyUtil.generateDSAKeypair(plength, qlength, random);
    }

}
