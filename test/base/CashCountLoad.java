package base;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.sql.SQLException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.rmj.appdriver.GRider;
import org.rmj.fund.manager.base.CashCount;
import org.rmj.fund.manager.base.LTransaction;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CashCountLoad {
    static GRider instance = new GRider();
    static CashCount trans;
    static LTransaction listener;
    
    public CashCountLoad() {
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
    public void test01SearchTransactionBranch() {
        try {
            if (trans.SearchTransaction("", false)){
                //get values
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }   
    }
    
    @Test
    public void test02SearchTransactionAudit() {
        try {
            if (trans.SearchTransaction("GMC Dagupan", false)){
                //get values
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }   
    }
}