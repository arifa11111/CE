/*******************************************************************************
 * Copyright 2018 T Mobile, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.tmobile.cloud.awsrules.iam;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import com.tmobile.cloud.awsrules.utils.CommonTestUtils;
import com.tmobile.cloud.awsrules.utils.IAMUtils;
import com.tmobile.cloud.awsrules.utils.PacmanUtils;
import com.tmobile.pacman.commons.exception.InvalidInputException;
import com.tmobile.pacman.commons.policy.BasePolicy;
@PowerMockIgnore({"javax.net.ssl.*","javax.management.*"})
@RunWith(PowerMockRunner.class)
@PrepareForTest({ PacmanUtils.class,IAMUtils.class})
public class AccessKeyRotatedRuleTest {

    @InjectMocks
    AccessKeyRotatedRule accessKeyRotatedRule;
    
    
    @Mock
    AmazonIdentityManagementClient identityManagementClient;

    @Before
    public void setUp() throws Exception{
        identityManagementClient = PowerMockito.mock(AmazonIdentityManagementClient.class); 
    }
    @Test
    public void test()throws Exception{
        Date date = new Date(); // Or where ever you get it from
        Date daysAgo = new DateTime(date).minusDays(300).toDate();
        AccessKeyMetadata accessKeyMetadata = new AccessKeyMetadata();
        accessKeyMetadata.setAccessKeyId("123");
        accessKeyMetadata.setCreateDate(daysAgo);
        accessKeyMetadata.setStatus("Active");
       
        List<AccessKeyMetadata> accessKeyMetadatas  = new ArrayList<>();
        accessKeyMetadatas.add(accessKeyMetadata);

        AccessKeyMetadata accessKeyMetadataTest = new AccessKeyMetadata();
        accessKeyMetadataTest.setAccessKeyId("123");
        accessKeyMetadataTest.setCreateDate(new Date());
        accessKeyMetadataTest.setStatus("Inactive");
       
        List<AccessKeyMetadata> accessKeyMetadatasTest  = new ArrayList<>();
        accessKeyMetadatasTest.add(accessKeyMetadataTest);
        
        List<AccessKeyMetadata> emptyAccessKeyMetadatas  = new ArrayList<>();
        
        mockStatic(PacmanUtils.class);
        when(PacmanUtils.doesAllHaveValue(anyString(),anyString())).thenReturn(
                true);
        
        
       
        
        Map<String,Object>map=new HashMap<String, Object>();
        map.put("client", identityManagementClient);
        AccessKeyRotatedRule spy = Mockito.spy(new AccessKeyRotatedRule());
        
        Mockito.doReturn(map).when((BasePolicy)spy).getClientFor(anyObject(), anyString(), anyObject());
        
        mockStatic(IAMUtils.class);
        when(IAMUtils.getAccessKeyInformationForUser(anyString(),anyObject())).thenReturn(accessKeyMetadatas);
        spy.execute(CommonTestUtils.getMapString("r_123 "),CommonTestUtils.getMapString("r_123 "));
        
        
        when(IAMUtils.getAccessKeyInformationForUser(anyString(),anyObject())).thenReturn(accessKeyMetadatasTest);
        spy.execute(CommonTestUtils.getMapString("r_123 "),CommonTestUtils.getMapString("r_123 "));
        
        when(IAMUtils.getAccessKeyInformationForUser(anyString(),anyObject())).thenReturn(emptyAccessKeyMetadatas);
        spy.execute(CommonTestUtils.getMapString("r_123 "),CommonTestUtils.getMapString("r_123 "));
        
        
        assertThatThrownBy( 
                () -> accessKeyRotatedRule.execute(CommonTestUtils.getMapString("r_123 "),CommonTestUtils.getMapString("r_123 "))).isInstanceOf(InvalidInputException.class);
        
        
        when(PacmanUtils.doesAllHaveValue(anyString(),anyString())).thenReturn(
                false);
        assertThatThrownBy(
                () -> accessKeyRotatedRule.execute(CommonTestUtils.getMapString("r_123 "),CommonTestUtils.getMapString("r_123 "))).isInstanceOf(InvalidInputException.class);
    }
  
    
    @Test
    public void getHelpTextTest(){
        assertThat(accessKeyRotatedRule.getHelpText(), is(notNullValue()));
    }

}
