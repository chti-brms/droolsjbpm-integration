/*
 *  Copyright 2010 salaboy.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.drools.grid.services.factory;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.drools.SystemEventListenerFactory;
//import org.drools.distributed.directory.impl.DistributedRioDirectoryConnector;
import org.drools.grid.local.LocalDirectoryConnector;
import org.drools.grid.remote.directory.RemoteMinaDirectoryConnector;

import org.drools.grid.services.DirectoryInstance;
import org.drools.grid.services.configuration.GenericProvider;
import org.drools.grid.services.configuration.MinaProvider;
import org.drools.grid.services.configuration.RioProvider;

public class DirectoryInstanceFactory {

    public static DirectoryInstance newDirectoryInstance(String name, GenericProvider provider) {
        return GenericProviderContainerFactoryHelper.doOnGenericProvider(provider, new DirectoryInstanceBuilder(name));
    }

    private static class DirectoryInstanceBuilder implements GenericProviderContainerBuilder<DirectoryInstance> {

        private String name;

        /**
         * @param directoryInstanceName the name for all directory instances created by this builder
         */
        public DirectoryInstanceBuilder(String directoryInstanceName) {
            this.name = directoryInstanceName;
        }

        public DirectoryInstance onLocalProvider() {
            return new DirectoryInstance(name, new LocalDirectoryConnector());
        }

        public DirectoryInstance onMinaProvider(MinaProvider provider) {
            return new DirectoryInstance(name,
                    new RemoteMinaDirectoryConnector(name,
                    ((MinaProvider) provider).getProviderAddress(),
                    ((MinaProvider) provider).getProviderPort(),
                    SystemEventListenerFactory.getSystemEventListener()));
        }

        public DirectoryInstance onRioProvider(RioProvider rioProvider) {
            throw new UnsupportedOperationException("Uncomment RIO DEPS and this method!");
//            try {
//                rioProvider.lookupDirectoryNodeServices();
//            } catch (IOException ex) {
//                Logger.getLogger(ExecutionEnvironmentFactory.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (InterruptedException ex) {
//                Logger.getLogger(ExecutionEnvironmentFactory.class.getName()).log(Level.SEVERE, null, ex);
//            }
//            return new DirectoryInstance(name,
//                    new DistributedRioDirectoryConnector(name,
//                            SystemEventListenerFactory.getSystemEventListener(),
//                            rioProvider.getDirectoryNode()));

        }

        public DirectoryInstance onHornetQProvider() {
            return null;
        }
    }
}
