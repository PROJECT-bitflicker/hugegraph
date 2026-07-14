/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hugegraph.api.auth;

import java.util.Arrays;
import java.util.List;

import org.apache.hugegraph.auth.AuthManager;
import org.apache.hugegraph.auth.HugeGroup;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import jakarta.ws.rs.ForbiddenException;

public class GraphSpaceGroupAPITest {

    @Test
    public void testSpaceManagerCanOnlyUseOwnedGraphSpaceEndpoint() {
        AuthManager auth = Mockito.mock(AuthManager.class);
        Mockito.when(auth.isAdminManager("manager")).thenReturn(false);
        Mockito.when(auth.isSpaceManager("SPACE_A", "manager"))
               .thenReturn(true);

        GraphSpaceGroupAPI.checkManagerPermission(auth, "SPACE_A",
                                                  "manager");
        Assert.assertThrows(ForbiddenException.class, () -> {
            GraphSpaceGroupAPI.checkManagerPermission(auth, "SPACE_B",
                                                      "manager");
        });
        Assert.assertThrows(ForbiddenException.class, () -> {
            GraphSpaceGroupAPI.checkManagerPermission(auth, "SPACE_A",
                                                      "ordinary");
        });
    }

    @Test
    public void testScopedGroupFilterNeverReturnsForeignOrLegacyGroup() {
        HugeGroup own = group("SPACE_A", 'a');
        HugeGroup foreign = group("SPACE_B", 'b');
        HugeGroup legacy = new HugeGroup("legacy-role");

        List<HugeGroup> filtered = GraphSpaceGroupAPI.filterScopedGroups(
                                   "SPACE_A",
                                   Arrays.asList(own, foreign, legacy));

        Assert.assertEquals(1, filtered.size());
        Assert.assertSame(own, filtered.get(0));
    }

    @Test
    public void testScopedGroupValidationRejectsForeignAndLegacyGroup() {
        HugeGroup own = group("SPACE_A", 'a');
        HugeGroup foreign = group("SPACE_B", 'b');
        HugeGroup legacy = new HugeGroup("legacy-role");

        GraphSpaceGroupAPI.checkScopedGroup("SPACE_A", own);
        Assert.assertThrows(ForbiddenException.class, () -> {
            GraphSpaceGroupAPI.checkScopedGroup("SPACE_A", foreign);
        });
        Assert.assertThrows(ForbiddenException.class, () -> {
            GraphSpaceGroupAPI.checkScopedGroup("SPACE_A", legacy);
        });
    }

    private static HugeGroup group(String graphSpace, char suffix) {
        return new HugeGroup(GraphSpaceGroupAPI.scopedPrefix(graphSpace) +
                             repeat(suffix, 32));
    }

    private static String repeat(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
