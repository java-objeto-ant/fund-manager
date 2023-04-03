/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.rmj.fund.manager.base;

import javax.sql.rowset.CachedRowSet;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.constants.EditMode;

/**
 *
 * @author User
 */
public class RaffleReport {
    
    private final GRider p_oApp;
    private final boolean p_bWithParent;
    
    
    private String p_sBranchCd;
    private int p_nEditMode;
    private int p_nTranStat;
    private String p_sMessage;
    private boolean p_bWithUI = true;

    
    private LMasDetTrans p_oListener;
    
    public RaffleReport(GRider foApp, String fsBranchCd, boolean fbWithParent){        
        p_oApp = foApp;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;        
                
        if (p_sBranchCd.isEmpty()) p_sBranchCd = p_oApp.getBranchCode();
        
        p_nTranStat = 0;
        p_nEditMode = EditMode.UNKNOWN;
    }
    
    public void setTranStat(int fnValue){
        p_nTranStat = fnValue;
    }
   
    public void setListener(LMasDetTrans foValue){
        p_oListener = foValue;
    }
    
    public void setWithUI(boolean fbValue){
        p_bWithUI = fbValue;
    }
    
    public int getEditMode(){
        return p_nEditMode;
    }
    
    
    private String getSQ_Record(){
        String lsSQL  = "SELECT " +
                        " c.sTransNox " +
                        " ,a.sAcctNmbr " +
                        " ,a.sAcctNmbr " +
                        " , g.sBranchCd " +
                        " , IFNULL(e.sCompnyNm, CONCAT(e.sLastName, ', ', e.sFrstName, ' ', IFNULL(e.sSuffixNm, ''), ' ', e.sMiddName)) xCompnyNm " +
                        " , g.sBranchNm " +
                        " , h.sPanaloDs  " +
                        " , f.sUserIDxx  " +
                        " , c.nAmountxx  " +
                        " , c.nItemQtyx  " +
                        " FROM  RaffleWinners a " +
                        " , ILMJ_Master b " +
                        "     LEFT JOIN ILMJ_Detail c " +
                        "       ON b.sTransNox = c.sTransNox " +
                        "     LEFT JOIN Panalo_Info h " +
                        "       ON c.sPanaloCd = h.sPanaloCD " +
                        " ,Employee_Master001 d " +
                        "     LEFT JOIN Client_Master e " +
                        "       ON  d.sEmployID = e.sClientID " +
                        "     LEFT JOIN App_User_Master f " +
                        "       ON  d.sEmployID = f.sEmployNo " +
                        "	   AND f.sProdctID = 'gRider' " +
                        "     LEFT JOIN Branch g " +
                        "       ON  d.sBranchCd = g.sBranchCd " +
                        " WHERE a.sAcctNmbr = e.sClientID " +
                        " AND a.sPrizexxx = c.cWinnerxx  " +
                        lsCondition();
        return lsSQL;
    }
    private String lsCondition(){
        String lsStat = String.valueOf(p_nTranStat);
        String lsCondition = "";
        if (lsStat.length() > 1){
            for (int lnCtr = 0; lnCtr <= lsStat.length()-1; lnCtr++){
                lsCondition += ", " + SQLUtil.toSQL(Character.toString(lsStat.charAt(lnCtr)));
            }
            
            lsCondition = " AND a.cTranStat IN (" + lsCondition.substring(2) + ")";
        } else{            
            lsCondition = " AND a.cTranStat = " + SQLUtil.toSQL(lsStat);
            
        }
        return lsCondition;
    }
}
