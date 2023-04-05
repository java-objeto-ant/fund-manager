/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.rmj.fund.manager.base;

import com.sun.rowset.CachedRowSetImpl;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.sql.RowSetMetaData;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import org.json.simple.JSONObject;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
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

    private CachedRowSet p_oRecord;
    
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
    public String getMessage(){
        return p_sMessage;
    }
    public int getItemCount() throws SQLException{
        p_oRecord.last();
        return p_oRecord.getRow();
    }
    public Object getRecord(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        if (getItemCount() == 0 || fnRow > getItemCount()) return null;
        
        p_oRecord.absolute(fnRow);
        return p_oRecord.getObject(fnIndex);
        
    }
    
    public Object getRecord(int fnRow, String fsIndex) throws SQLException{
        return getRecord(fnRow, getColumnIndex(p_oRecord, fsIndex));
    }
    
    public void setMaster(String fsIndex, Object foValue) throws SQLException{        
        setMaster(getColumnIndex(p_oRecord,fsIndex), foValue);
    }
    
    public void setMaster(int fnIndex, Object foValue) throws SQLException{
        p_oRecord.first();
        switch (fnIndex){
            default:
                p_oRecord.updateString(fnIndex, ((String) foValue).trim());
                p_oRecord.updateRow();

                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oRecord.getString(fnIndex));
                break;
        }
    }
    
    
    public boolean SearchRecord(String fsValue, boolean fbByCode) throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        p_sMessage = "";
        String lsSQL = getSQL_ILMJMaster();
        String lsCondition = "";
        if(fbByCode){
            lsCondition = " sTransNox = " + SQLUtil.toSQL(fsValue);
        }else{
            lsCondition = " dRaffleDt LIKE " + SQLUtil.toSQL(fsValue + "%");
        }
        if (!lsCondition.isEmpty()) lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        if (p_bWithUI){
            JSONObject loJSON = showFXDialog.jsonSearch(
                                p_oApp, 
                                lsSQL, 
                                fsValue, 
                                "Trans. No.»Branch»Period»Remarks", 
                                "a.sTransNox»xBranchNm»a.sMonthxxx»a.sRemarksx", 
                                "a.sTransNox»c.sBranchNm»a.sMonthxxx»a.sRemarksx", 
                                fbByCode ? 0 : 1);
            
            if (loJSON != null) 
                return OpenRecord((String) loJSON.get("dRaffleDt"));
            else {
                p_sMessage = "No record selected.";
                return false;
            }
        }
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "sTransNox = " + SQLUtil.toSQL(fsValue));   
        else {
            lsSQL = MiscUtil.addCondition(lsSQL, "dRaffleDt LIKE " + SQLUtil.toSQL(fsValue + "%")); 
            lsSQL += " LIMIT 1";
        }
        
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        if (!loRS.next()){
            MiscUtil.close(loRS);
            p_sMessage = "No transaction found for the givern criteria.";
            return false;
        }
        
        lsSQL = loRS.getString("dRaffleDt");
        lsSQL = getDate(lsSQL);
        MiscUtil.close(loRS);
        
        return OpenRecord(lsSQL);
        
    }
    public boolean OpenRecord(String fsValue) throws SQLException{
         
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
//        createDetail();
        p_sMessage = ""; 
        String lsSQL = getSQL_Record();
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();
        String lsCondition ="";
        String lsCondition1 ="";
         
      if(!fsValue.isEmpty()){
           lsCondition =  lsCondition + " AND a.dModified LIKE " +SQLUtil.toSQL(fsValue + "%");
        }
        lsSQL = lsSQL + lsCondition + " GROUP BY a.sAcctNmbr ORDER BY a.cTranstat DESC";
        System.out.println(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
       
        p_oRecord = factory.createCachedRowSet();
        p_oRecord.populate(loRS);
        MiscUtil.close(loRS);
        setRaffleNames();
        p_nEditMode = EditMode.READY;
        return true;
    }
    private void setRaffleNames() throws SQLException{
        int lnCtr;
        for(lnCtr = 1; lnCtr <= getItemCount(); lnCtr++){
            
            p_oRecord.absolute(lnCtr);
            String lsSQL;
            ResultSet loRS;
            if(p_oRecord.getString("sAcctNmbr").length() == 10){
                 lsSQL = "SELECT" +
                   " IFNULL(b.sCompnyNm, CONCAT(b.sLastName, ', ', b.sFrstName, ' ', IFNULL(b.sSuffixNm, ''), ' ', b.sMiddName)) xCompnyNm" +
                   ", c.sBranchNm " +
                   ", b.sMobileNo " +
                   ", TRIM(CONCAT(IFNULL(b.sHouseNox,''),' ',IFNULL(b.sAddressx,''),' ',IFNULL(f.sBrgyName,''),', ',IFNULL(d.sTownName,''),', ',IFNULL(e.sProvName,''))) sAddressx " +
                " FROM MC_AR_Master a" +
                   ", Client_Master b" +
                   "    LEFT JOIN TownCity d" +
                   "        ON b.sTownIDxx = d.sTownIDxx " +
                   "    LEFT JOIN Province e" +
                   "        ON d.sProvIDxx = e.sProvIDxx " +
                   "    LEFT JOIN Barangay f" +
                   "        ON b.sTownIDxx = f.sTownIDxx " +
                        "        AND b.sBrgyIDxx = f.sBrgyIDxx " +
                   ", Branch c" +
                " WHERE a.sClientID = b.sClientID" +
                   " AND a.sBranchCd = c.sBranchCd" +
                   " AND a.sAcctNmbr = " + SQLUtil.toSQL(p_oRecord.getString("sAcctNmbr"));
            }else{
                lsSQL = "SELECT" +
                   " IFNULL(b.sCompnyNm, CONCAT(b.sLastName, ', ', b.sFrstName, ' ', IFNULL(b.sSuffixNm, ''), ' ', b.sMiddName)) xCompnyNm" +
                   ", c.sBranchNm " +
                   ", b.sMobileNo " +
                   ", TRIM(CONCAT(IFNULL(b.sHouseNox,''),' ',IFNULL(b.sAddressx,''),' ',IFNULL(f.sBrgyName,''),', ',IFNULL(d.sTownName,''),', ',IFNULL(e.sProvName,''))) sAddressx " +
                " FROM Client_Master b" +
                   "    LEFT JOIN TownCity d" +
                   "        ON b.sTownIDxx = d.sTownIDxx " +
                   "    LEFT JOIN Province e" +
                   "        ON d.sProvIDxx = e.sProvIDxx " +
                   "    LEFT JOIN Barangay f" +
                   "        ON b.sTownIDxx = f.sTownIDxx " +
                        "        AND b.sBrgyIDxx = f.sBrgyIDxx " +
                " , Branch c" +
                " WHERE b.sClientID = " + SQLUtil.toSQL(p_oRecord.getString("sAcctNmbr"));
            }
             loRS = p_oApp.executeQuery(lsSQL);    
             if(loRS.next()){
                 System.out.println(p_oRecord.getString("cGroupNox"));
                p_oRecord.updateString("xAddressx", loRS.getString("sAddressx"));
                p_oRecord.updateString("sCompanyNm", loRS.getString("xCompnyNm"));
                p_oRecord.updateString("sBranchNme", loRS.getString("sBranchNm"));
                p_oRecord.updateString("sMobileNox", loRS.getString("sMobileNo"));
                p_oRecord.updateRow();
             }
             MiscUtil.close(loRS);
        }
        displayDetFields();
    } 
//    private void createDetail() throws SQLException{
//        RowSetMetaData meta = new RowSetMetaDataImpl();        
//
//        meta.setColumnCount(9);
//        
//        
//        meta.setColumnName(1, "sAcctNmbr");
//        meta.setColumnLabel(1, "sAcctNmbr");
//        meta.setColumnType(1, Types.VARCHAR);
//        meta.setColumnDisplaySize(1, 12);
//        
//        meta.setColumnName(2, "sSourceCd");
//        meta.setColumnLabel(2, "sSourceCd");
//        meta.setColumnType(2, Types.VARCHAR);
//        meta.setColumnDisplaySize(2, 4);
//        
//        meta.setColumnName(3, "cCallStat");
//        meta.setColumnLabel(3, "cCallStat");
//        meta.setColumnType(3, Types.VARCHAR);
//        meta.setColumnDisplaySize(3, 2);
//        
//        meta.setColumnName(4, "cTranstat");
//        meta.setColumnLabel(4, "cTranstat");
//        meta.setColumnType(4, Types.VARCHAR);
//        meta.setColumnDisplaySize(4, 1);
//        
//        meta.setColumnName(5, "sPrizexxx");
//        meta.setColumnLabel(5, "sPrizexxx");
//        meta.setColumnType(5, Types.VARCHAR);
//        meta.setColumnDisplaySize(5, 1);
//        
//        meta.setColumnName(6, "sCompanyNm");
//        meta.setColumnLabel(6, "sCompanyNm");
//        meta.setColumnType(6, Types.VARCHAR);
//        
//        meta.setColumnName(7, "sBranchNme");
//        meta.setColumnLabel(7, "sBranchNme");
//        meta.setColumnType(7, Types.VARCHAR);
//        
//        meta.setColumnName(8, "sMobileNox");
//        meta.setColumnLabel(8, "sMobileNox");
//        meta.setColumnType(8, Types.VARCHAR);
//        
//        meta.setColumnName(9, "sPanaloDs");
//        meta.setColumnLabel(9, "sPanaloDs");
//        meta.setColumnType(9, Types.VARCHAR);
//        
//        p_oRecord = new CachedRowSetImpl();
//        p_oRecord.setMetaData(meta); 
//    }
    private String getSQL_Record(){
        String lsSQL = "SELECT a.sAcctNmbr" +
                ", a.sSourceCd " +
                ", a.cCallStat " +
                ", a.cTranstat " +
                ", a.sPrizexxx " +
                ", '' sCompanyNm " +
                ", '' sBranchNme " +
                ", '' sMobileNox " +
                ", '' xAddressx " +
                ", a.sRaffleNo " +
                ", c.nAmountxx " +
                ", c.nItemQtyx " +
                ", d.sPanaloDs " +
                ", CEIL((ROW_NUMBER() OVER())/3) AS cGroupNox " +
                " FROM RaffleWinners a"+
                " , ILMJ_Master b " +
                "     LEFT JOIN ILMJ_Detail c " +
                "       ON b.sTransNox = c.sTransNox " +
                "     LEFT JOIN Panalo_Info d " +
                "       ON c.sPanaloCd = d.sPanaloCD ";
        lsSQL = MiscUtil.addCondition(lsSQL, lsCondition());
        
        return lsSQL;
    }
    private String getSQL_ILMJMaster(){
         String lsSQL = "SELECT " +
                " sTransNox" +
                ", dRaffleDt " +
                ", sRemarksx " +
                " FROM ILMJ_Master ";
        return lsSQL;
    }
    private String lsCondition(){
        String lsStat = String.valueOf(p_nTranStat);
        String lsCondition = "";
        if (lsStat.length() > 1){
            for (int lnCtr = 0; lnCtr <= lsStat.length()-1; lnCtr++){
                lsCondition += ", " + SQLUtil.toSQL(Character.toString(lsStat.charAt(lnCtr)));
            }
            
            lsCondition = " a.cTranStat IN (" + lsCondition.substring(2) + ")";
        } else{            
            lsCondition = " a.cTranStat = " + SQLUtil.toSQL(lsStat);
            
        }
        return lsCondition;
    }
    
    private int getColumnIndex(CachedRowSet loRS, String fsValue) throws SQLException{
        int lnIndex = 0;
        int lnRow = loRS.getMetaData().getColumnCount();
        
        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            if (fsValue.equals(loRS.getMetaData().getColumnLabel(lnCtr))){
                lnIndex = lnCtr;
                break;
            }
        }
        
        return lnIndex;
    }
    public void displayDetFields() throws SQLException{
        int lnRow = p_oRecord.getMetaData().getColumnCount();
        
        System.out.println("----------------------------------------");
        System.out.println("DETAIL TABLE INFO");
        System.out.println("----------------------------------------");
        System.out.println("Total number of columns: " + lnRow);
        System.out.println("----------------------------------------");
        
        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            System.out.println("Column index: " + (lnCtr) + " --> Label: " + p_oRecord.getMetaData().getColumnLabel(lnCtr));
            if (p_oRecord.getMetaData().getColumnType(lnCtr) == Types.CHAR ||
                p_oRecord.getMetaData().getColumnType(lnCtr) == Types.VARCHAR){
                
                System.out.println("Column index: " + (lnCtr) + " --> Size: " + p_oRecord.getMetaData().getColumnDisplaySize(lnCtr));
            }
        }
        
        System.out.println("Record Count == " + getItemCount());
        System.out.println("----------------------------------------");
        System.out.println("END: DETAIL TABLE INFO");
        System.out.println("----------------------------------------");
    }
    
    public static String getDate (String dtransact) {
       
        try {
            Date date = new Date();
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            date = (Date)formatter.parse(dtransact);  
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
            String todayStr = fmt.format(date);
            
            return todayStr;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
    
}
