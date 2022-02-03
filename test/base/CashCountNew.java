package base;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.sql.SQLException;
import java.util.Date;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.SQLUtil;
import org.rmj.fund.manager.base.CashCount;
import org.rmj.fund.manager.base.LTransaction;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CashCountNew {
    static GRider instance = new GRider();
    static CashCount trans;
    static LTransaction listener;
    
    public CashCountNew() {
    }
    
    @BeforeClass
    public static void setUpClass() {        
        if (!instance.logUser("gRider", "M001111122")){
            System.err.println(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
        
        listener = new LTransaction() {
            @Override
            public void MasterRetreive(int fnIndex, Object foValue) {
                System.out.println(fnIndex + "-->" + foValue);
            }

        };
        
        trans = new CashCount(instance, instance.getBranchCode(), false);
        trans.setListener(listener);
        trans.setWithUI(false);
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    @Test
    public void test01NewTransaction() {
        try {
            if (trans.NewTransaction()){
                trans.displayMasFields();       
         
                trans.setMaster("dTransact", SQLUtil.toDate("2022-01-29", SQLUtil.FORMAT_SHORT_DATE));
                System.out.println(SQLUtil.dateFormat((Date) trans.getMaster("dTransact"), SQLUtil.FORMAT_MEDIUM_DATE));
                
                trans.setMaster("nCn0001cx", 1);
                assertTrue(1 == (int) trans.getMaster("nCn0001cx"));
                
                trans.setMaster("nCn0005cx", 2);
                assertTrue(2 == (int) trans.getMaster("nCn0005cx"));
                
                trans.setMaster("nCn0010cx", 2);
                assertTrue(2 == (int) trans.getMaster("nCn0010cx"));
                
                trans.setMaster("nCn0025cx", 3);
                assertTrue(3 == (int) trans.getMaster("nCn0025cx"));
                
                trans.setMaster("nCn0001px", 4);
                assertTrue(4 == (int) trans.getMaster("nCn0001px"));
                
                trans.setMaster("nCn0005px", 5);
                assertTrue(5 == (int) trans.getMaster("nCn0005px"));
                
                trans.setMaster("nCn0010px", 6);
                assertTrue(6 == (int) trans.getMaster("nCn0010px"));
                
                trans.setMaster("nCn0020px", 6);
                assertTrue(6 == (int) trans.getMaster("nCn0020px"));
                
                trans.setMaster("nNte0020p", 7);
                assertTrue(7 == (int) trans.getMaster("nNte0020p"));
                
                trans.setMaster("nNte0050p", 8);
                assertTrue(8 == (int) trans.getMaster("nNte0050p"));
                
                trans.setMaster("nNte0100p", 9);
                assertTrue(9 == (int) trans.getMaster("nNte0100p"));
                
                trans.setMaster("nNte0200p", 10);
                assertTrue(10 == (int) trans.getMaster("nNte0200p"));
                
                trans.setMaster("nNte0500p", 11);
                assertTrue(11 == (int) trans.getMaster("nNte0500p"));
                
                trans.setMaster("nNte0500p", 11);
                assertTrue(11 == (int) trans.getMaster("nNte0500p"));
                
                trans.setMaster("sORNoxxxx", "0001");
                assertTrue("0001".equals((String) trans.getMaster("sORNoxxxx")));
                
                trans.setMaster("sSINoxxxx", "0002");
                assertTrue("0002".equals((String) trans.getMaster("sSINoxxxx")));
                
                trans.setMaster("sPRNoxxxx", "0003");
                assertTrue("0003".equals((String) trans.getMaster("sPRNoxxxx")));
                
                trans.setMaster("sCRNoxxxx", "0004");
                assertTrue("0004".equals((String) trans.getMaster("sCRNoxxxx")));
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }   
    }
    
    @Test
    public void test02SaveTransaction() {
        try {
            if (trans.SaveTransaction()){
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        
    }
}