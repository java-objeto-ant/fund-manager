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
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    @Test
    public void test01SearchALLRecord() {
        try {
            trans.setTranStat(10);
            if (trans.SearchRecord("M00123000002", true)){
                
                    System.out.println("---------- START ALL RECORD ----------");
                for (int lnCtr = 1; lnCtr <= trans.getItemCount(); lnCtr++){
                    System.out.println();
                    System.out.println(trans.getRecord(lnCtr,"sAcctNmbr"));
                    System.out.println(trans.getRecord(lnCtr,"sSourceCd"));
                    System.out.println(trans.getRecord(lnCtr,"sCompanyNm"));
                    System.out.println(trans.getRecord(lnCtr,"sBranchNme"));
                    System.out.println(trans.getRecord(lnCtr,"sMobileNox"));
                    System.out.println(trans.getRecord(lnCtr,"sPanaloDs"));
                    System.out.println(StringHelper.prepad((String) trans.getRecord(lnCtr,"sRaffleNo"), 6, '0'));
                }
                    System.out.println("---------- END ALL RECORD ----------");
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }   
    }


    @Test
    public void test02SearchWinnerRecord() {
        try {
            trans.setTranStat(1);
            if (trans.SearchRecord("M00123000002", true)){
                
                    System.out.println("---------- START WINNER RECORD ----------");
                for (int lnCtr = 1; lnCtr <= trans.getItemCount(); lnCtr++){
                    System.out.println();
                    System.out.println(trans.getRecord(lnCtr,"sAcctNmbr"));
                    System.out.println(trans.getRecord(lnCtr,"sSourceCd"));
                    System.out.println(trans.getRecord(lnCtr,"sCompanyNm"));
                    System.out.println(trans.getRecord(lnCtr,"sBranchNme"));
                    System.out.println(trans.getRecord(lnCtr,"sMobileNox"));
                    System.out.println(trans.getRecord(lnCtr,"sPanaloDs"));
                    System.out.println(StringHelper.prepad((String) trans.getRecord(lnCtr,"sRaffleNo"), 6, '0'));
                }
                    System.out.println("---------- END WINNER RECORD ----------");
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }   
    }

    @Test
    public void test03SearchNonWinnerRecord() {
        try {
            trans.setTranStat(0);
            if (trans.SearchRecord("M00123000002", true)){
                
                    System.out.println("---------- START NON-WINNER RECORD ----------");
                for (int lnCtr = 1; lnCtr <= trans.getItemCount(); lnCtr++){
                    System.out.println();
                    System.out.println(trans.getRecord(lnCtr,"sAcctNmbr"));
                    System.out.println(trans.getRecord(lnCtr,"sSourceCd"));
                    System.out.println(trans.getRecord(lnCtr,"sCompanyNm"));
                    System.out.println(trans.getRecord(lnCtr,"sBranchNme"));
                    System.out.println(trans.getRecord(lnCtr,"sMobileNox"));
                    System.out.println(trans.getRecord(lnCtr,"sPanaloDs"));
                    System.out.println(StringHelper.prepad((String) trans.getRecord(lnCtr,"sRaffleNo"), 6, '0'));
                }
                    System.out.println("---------- END NON-WINNER RECORD ----------");
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }   
    }

}
