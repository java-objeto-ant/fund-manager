/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package base;

import static base.IncentiveApproval.instance;
import static base.RaffleReports.instance;
import java.sql.SQLException;
import org.junit.AfterClass;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.rmj.appdriver.GRider;
import org.rmj.fund.manager.base.MCImages;

/**
 *
 * @author User
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MCImage {
    static GRider instance = new GRider();
    static MCImages trans;
    
    public MCImage() {
    }
    
    @BeforeClass
    public static void setUpClass() {     
        
        if (!instance.loadEnv("gRider")) {
            System.err.println(instance.getErrMsg());
            System.exit(1);
        }
        if (!instance.logUser("gRider", "M001111122")){
            System.err.println(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
        
        
        trans = new MCImages(instance, instance.getBranchCode(), false);
        trans.setWithUI(false);
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Test
    public void test01NewTransaction() {
        try {
            if (trans.NewTransaction()){
//                trans.displayMasFields();                
                
                System.out.println(trans.getImageCount() + 1);
                for (int lnCtr = 1; lnCtr <= trans.getImageCount(); lnCtr++){
                    System.out.println(trans.getDetail(lnCtr,"sMCInvIDx"));
                    System.out.println(trans.getDetail(lnCtr,"nEntryNox"));
                }
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }   
    }
    @Test
    public void test02SearchItem() {
        try {
            if (trans.searchItem(0, "", false, false)){
//                trans.displayMasFields();                
                
                System.out.println(trans.getImageCount() + 1);
                for (int lnCtr = 1; lnCtr <= trans.getImageCount(); lnCtr++){
                    System.out.println(trans.getDetail(lnCtr,"sMCInvIDx"));
                    System.out.println(trans.getDetail(lnCtr,"nEntryNox"));
                }
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }   
    }

    
}
