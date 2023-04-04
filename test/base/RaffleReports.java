/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package base;

import static base.Release.trans;
import java.sql.SQLException;
import org.junit.AfterClass;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.StringHelper;
import org.rmj.fund.manager.base.RaffleReport;

/**
 *
 * @author User
 */
public class RaffleReports {
    static GRider instance = new GRider();
    static RaffleReport trans;
    
    public RaffleReports() {
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
        
        trans = new RaffleReport(instance, instance.getBranchCode(), false);
        trans.setWithUI(false);
        trans.setTranStat(10);
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    @Test
    public void test01SearchRecord() {
        try {
            if (trans.SearchRecord("M00123000002", true)){
                
                for (int lnCtr = 1; lnCtr <= trans.getItemCount(); lnCtr++){
                    System.out.println(trans.getRecord(lnCtr,"sAcctNmbr"));
                    System.out.println(trans.getRecord(lnCtr,"sSourceCd"));
                    System.out.println(trans.getRecord(lnCtr,"sCompanyNm"));
                    System.out.println(trans.getRecord(lnCtr,"sBranchNme"));
                    System.out.println(trans.getRecord(lnCtr,"sMobileNox"));
                    System.out.println(trans.getRecord(lnCtr,"sPanaloDs"));
                    System.out.println(StringHelper.prepad((String) trans.getRecord(lnCtr,"sRaffleNo"), 6, '0'));
                }
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }   
    }

}
