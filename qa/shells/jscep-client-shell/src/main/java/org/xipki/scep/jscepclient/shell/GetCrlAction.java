/*
 *
 * Copyright (c) 2013 - 2018 Lijun Liao
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

package org.xipki.scep.jscepclient.shell;

import java.io.File;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.jscep.client.Client;
import org.xipki.security.util.X509Util;
import org.xipki.shell.CmdFailure;
import org.xipki.shell.completer.DerPemCompleter;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "xi", name = "jscep-getcrl", description = "download CRL")
@Service
public class GetCrlAction extends ClientAction {

  @Option(name = "--cert", aliases = "-c", required = true, description = "certificate")
  @Completion(FileCompleter.class)
  private String certFile;

  @Option(name = "--outform", description = "output format of the CRL")
  @Completion(DerPemCompleter.class)
  protected String outform = "der";

  @Option(name = "--out", aliases = "-o", required = true,
      description = "where to save the CRL")
  @Completion(FileCompleter.class)
  private String outputFile;

  @Override
  protected Object execute0() throws Exception {
    X509Certificate cert = X509Util.parseCert(new File(certFile));
    Client client = getScepClient();
    X509CRL crl = client.getRevocationList(getIdentityCert(), getIdentityKey(),
        cert.getIssuerX500Principal(), cert.getSerialNumber());
    if (crl == null) {
      throw new CmdFailure("received no CRL from server");
    }

    saveVerbose("saved CRL to file", outputFile, encodeCrl(crl.getEncoded(), outform));
    return null;
  }

}
