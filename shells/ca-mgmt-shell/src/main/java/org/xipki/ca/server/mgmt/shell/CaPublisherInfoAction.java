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

package org.xipki.ca.server.mgmt.shell;

import java.util.List;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.xipki.ca.server.mgmt.api.PublisherEntry;
import org.xipki.ca.server.mgmt.shell.completer.CaNameCompleter;
import org.xipki.shell.CmdFailure;
import org.xipki.util.StringUtil;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "ca", name = "capub-info",
    description = "show information of publisher in given CA")
@Service
public class CaPublisherInfoAction extends CaAction {

  @Option(name = "--ca", required = true, description = "CA name")
  @Completion(CaNameCompleter.class)
  private String caName;

  @Override
  protected Object execute0() throws Exception {
    if (caManager.getCa(caName) == null) {
      throw new CmdFailure("could not find CA '" + caName + "'");
    }

    List<PublisherEntry> entries = caManager.getPublishersForCa(caName);
    if (isNotEmpty(entries)) {
      StringBuilder sb = new StringBuilder();
      sb.append("publishers for CA ").append(caName).append("\n");
      for (PublisherEntry entry : entries) {
        sb.append("\t").append(entry.getIdent().getName()).append("\n");
      }
      println(sb.toString());
    } else {
      println(StringUtil.concat("no publisher for CA ", caName," is configured"));
    }

    return null;
  }

}
