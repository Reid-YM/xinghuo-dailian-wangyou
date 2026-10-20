package com.xinghuo.wecom;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class XingweiWeComMemberStatusTest {
    @Test void onlyActiveMemberCanProduceLoginIdentityAndMissingNameNeverLeaksId() throws Exception {
        var mapper=new ObjectMapper();
        var member=mapper.createObjectNode().put("userid","private-id").put("status",1).put("enable",1);
        assertEquals("企业微信员工",XingweiWeComDirectoryClient.verifiedMember("private-id",member).name());
        for(int status:new int[]{2,4,5,0}){
            member.put("status",status);
            assertThrows(IllegalStateException.class,()->XingweiWeComDirectoryClient.verifiedMember("private-id",member));
        }
        member.put("status",1).put("enable",0);
        assertThrows(IllegalStateException.class,()->XingweiWeComDirectoryClient.verifiedMember("private-id",member));
        member.put("enable",1);
        assertThrows(IllegalStateException.class,()->XingweiWeComDirectoryClient.verifiedMember("other-id",member));
    }
}
