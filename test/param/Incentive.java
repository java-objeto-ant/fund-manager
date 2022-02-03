package param;

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

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Incentive {
    static GRider instance = new GRider();
    static org.rmj.fund.manager.parameters.Incentive trans;
    
    public Incentive() {
    }
    
    @BeforeClass
    public static void setUpClass() {        
        if (!instance.logUser("gRider", "M001111122")){
            System.err.println(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
        
        trans = new org.rmj.fund.manager.parameters.Incentive(instance, instance.getBranchCode(), false);
        trans.setWithUI(false);
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    @Test
    public void test01NewTransaction() {
        try {
            if (trans.NewRecord()){
                trans.displayMasFields();
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }
        
    }
    
    @Test 
    public void test02SetMaster(){
        try {
            trans.setMaster("sInctveDs", "Sample Incentive");
            assertEquals("Sample Incentive", (String) trans.getMaster("sInctveDs"));
            
            trans.setMaster("sDivision", "123");
            assertEquals("123", (String) trans.getMaster("sDivision"));
            
            trans.setMaster("cInctveTp", "2");
            assertEquals("2", (String) trans.getMaster("cInctveTp"));
            
            trans.setMaster("cByPercnt", "1");
            assertEquals("1", (String) trans.getMaster("cByPercnt"));
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test 
    public void test03SaveRecord(){
        try {
            if (!trans.SaveRecord())
                fail(trans.getMessage());
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test 
    public void test04Search(){
        try {
            if (trans.SearchRecord("Sample Incenti", false)){
                if (trans.UpdateRecord()){
                    trans.setMaster("sDivision", "0");
                    assertEquals("0", (String) trans.getMaster("sDivision"));

                    trans.setMaster("cInctveTp", "0");
                    assertEquals("0", (String) trans.getMaster("cInctveTp"));

                    trans.setMaster("cByPercnt", "0");
                    assertEquals("0", (String) trans.getMaster("cByPercnt"));
                } else
                    fail(trans.getMessage());
            } else
                fail(trans.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    @Test 
    public void test05SaveRecord(){
        try {
            if (!trans.SaveRecord())
                fail(trans.getMessage());
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test 
    public void test06Search(){
        try {
            if (trans.SearchRecord("Sample Incenti", false)){
                if (trans.DeactivateRecord()){
                } else
                    fail(trans.getMessage());
            } else
                fail(trans.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    @Test 
    public void test07Search(){
        try {
            if (trans.SearchRecord("Sample Incenti", false)){
                if (trans.ActivateRecord()){
                } else
                    fail(trans.getMessage());
            } else
                fail(trans.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}