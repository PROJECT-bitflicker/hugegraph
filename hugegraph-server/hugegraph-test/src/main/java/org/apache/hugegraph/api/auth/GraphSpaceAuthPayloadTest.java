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

import java.util.Map;

import org.apache.hugegraph.auth.AuthManager;
import org.apache.hugegraph.auth.HugeAccess;
import org.apache.hugegraph.auth.HugeBelong;
import org.apache.hugegraph.auth.HugePermission;
import org.apache.hugegraph.auth.HugeTarget;
import org.apache.hugegraph.backend.id.IdGenerator;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ForbiddenException;

public class GraphSpaceAuthPayloadTest {

    @Test
    public void testTargetPayloadUsesPathGraphSpaceAndOptionalUrl()
            throws Exception {
        TargetAPI.JsonTarget jsonTarget = new ObjectMapper().readValue(
                "{\"target_name\":\"target\"," +
                "\"target_graph\":\"hugegraph\"," +
                "\"target_description\":\"description\"," +
                "\"target_resources\":[]}",
        TargetAPI.JsonTarget.class);

        HugeTarget target = jsonTarget.build("SPACE_A");
        target.creator("manager");
        Map<String, Object> properties = target.asMap();

        Assert.assertEquals("SPACE_A", target.graphSpace());
        Assert.assertEquals("description", target.description());
        Assert.assertEquals("", target.url());
        Assert.assertEquals("SPACE_A", properties.get("graphspace"));
        Assert.assertEquals("description",
                            properties.get("target_description"));
    }

    @Test
    public void testRelationshipPayloadsUsePathGraphSpace() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AccessAPI.JsonAccess jsonAccess = mapper.readValue(
                "{\"group\":\"group\",\"target\":\"target\"," +
                "\"access_permission\":\"READ\"}",
                AccessAPI.JsonAccess.class);
        HugeAccess access = jsonAccess.build("SPACE_A");

        Assert.assertEquals("SPACE_A", access.graphSpace());
        Assert.assertEquals(HugePermission.READ, access.permission());

        BelongAPI.JsonBelong jsonBelong = mapper.readValue(
                "{\"user\":\"user\",\"group\":\"group\"}",
                BelongAPI.JsonBelong.class);
        HugeBelong belong = jsonBelong.build("SPACE_A");

        Assert.assertEquals("SPACE_A", belong.graphSpace());
        Assert.assertEquals(HugeBelong.UG, belong.link());
    }

    @Test
    public void testTargetRejectsForeignGraphSpace() {
        HugeTarget target = new HugeTarget("target", "hugegraph", "");
        target.graphSpace("SPACE_A");

        TargetAPI.checkGraphSpace("SPACE_A", target);
        Assert.assertThrows(ForbiddenException.class, () ->
                TargetAPI.checkGraphSpace("SPACE_B", target));
    }

    @Test
    public void testAccessRejectsForeignGraphSpace() {
        HugeAccess access = new HugeAccess("SPACE_A",
                                           IdGenerator.of("group"),
                                           IdGenerator.of("target"));

        AccessAPI.checkGraphSpace("SPACE_A", access);
        Assert.assertThrows(ForbiddenException.class, () ->
                AccessAPI.checkGraphSpace("SPACE_B", access));
    }

    @Test
    public void testCreateAccessRejectsForeignTargetWithoutSideEffects()
            throws Exception {
        AuthManager auth = Mockito.mock(AuthManager.class);
        AccessAPI.JsonAccess jsonAccess = new ObjectMapper().readValue(
                "{\"group\":\"group\",\"target\":\"target\"," +
                "\"access_permission\":\"READ\"}",
                AccessAPI.JsonAccess.class);
        HugeAccess access = jsonAccess.build("SPACE_A");
        HugeTarget target = new HugeTarget("target", "hugegraph", "");
        target.graphSpace("SPACE_B");
        Mockito.when(auth.getTarget("SPACE_A", access.target()))
               .thenReturn(target);

        Assert.assertThrows(ForbiddenException.class, () ->
                AccessAPI.createScopedAccess(auth, "SPACE_A", access));

        Mockito.verify(auth).getTarget("SPACE_A", access.target());
        Mockito.verify(auth, Mockito.never())
               .createAccess(Mockito.anyString(),
                             Mockito.any(HugeAccess.class));
    }

    @Test
    public void testBelongRejectsForeignGraphSpace() {
        HugeBelong belong = new HugeBelong("SPACE_A",
                                           IdGenerator.of("user"),
                                           IdGenerator.of("group"),
                                           null, HugeBelong.UG);

        BelongAPI.checkGraphSpace("SPACE_A", belong);
        Assert.assertThrows(ForbiddenException.class, () ->
                BelongAPI.checkGraphSpace("SPACE_B", belong));
    }
}
