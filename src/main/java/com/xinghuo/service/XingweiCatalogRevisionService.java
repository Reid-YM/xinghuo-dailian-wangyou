package com.xinghuo.service;

import com.xinghuo.exception.NotFoundException;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.function.Supplier;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

/** Shared editor revisions prevent basic and full-profile editors overwriting one another. */
@Service
public class XingweiCatalogRevisionService {
    public enum Kind {
        SERVICE("services","id"),GAME("game_catalog","code"),CATEGORY("service_categories","id"),QUOTE("quotes","id"),ORDER("customer_orders","order_no");
        final String table,key;Kind(String table,String key){this.table=table;this.key=key;}
    }
    private final JdbcTemplate jdbc;private final EntityManager entities;
    public XingweiCatalogRevisionService(JdbcTemplate jdbc,EntityManager entities){this.jdbc=jdbc;this.entities=entities;}
    @Transactional(readOnly=true) public String current(Kind kind,Object id){return read(kind,id,false);}
    public void lock(Kind kind,Object id,String expected){String actual=read(kind,id,true);if(expected==null||(!actual.equals(expected)&&!("\""+actual+"\"").equals(expected)))throw new OptimisticLockingFailureException("记录已变化，请重新读取核对；当前输入尚未保存");}
    @Transactional(isolation=Isolation.READ_COMMITTED) public <T> T change(Kind kind,Object id,String expected,Supplier<T> action){lock(kind,id,expected);T result=action.get();entities.flush();return result;}
    @Transactional(isolation=Isolation.READ_COMMITTED) public <T> T serialized(Kind kind,Object id,Supplier<T> action){read(kind,id,true);T result=action.get();entities.flush();return result;}
    private String read(Kind kind,Object id,boolean lock){var rows=jdbc.query("SELECT updated_at"+(kind==Kind.SERVICE?",version":"")+" FROM "+kind.table+" WHERE "+kind.key+"=?"+(lock?" FOR UPDATE":""),(rs,n)->kind==Kind.SERVICE?"v"+rs.getLong(2):digest(kind.name()+":"+id+":"+rs.getTimestamp(1)),id);if(rows.isEmpty())throw new NotFoundException();return rows.getFirst();}
    private String digest(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException error){throw new IllegalStateException(error);}}
}
