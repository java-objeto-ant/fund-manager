/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.rmj.fund.manager.base;

import com.sun.rowset.CachedRowSetImpl;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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
import org.rmj.appdriver.constants.RecordStatus;

/**
 *
 * @author User
 */
public class PanaloRedemptionReport {
   private final String MASTER_TABLE = "Guanzon_Panalo_Redemption";
   private final String DETAIL_TABLE = "Panalo_Info";
    
    private final GRider p_oApp;
    private final boolean p_bWithParent;
    
    private String p_sBranchCd;
    private int p_nEditMode;

    private String p_sMessage;
    private boolean p_bWithUI = true;
    
    private LMasDetTrans p_oListener;
    
    private CachedRowSet p_oRecord;
    
    public PanaloRedemptionReport(GRider foApp, String fsBranchCd, boolean fbWithParent){
        p_oApp = foApp;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;        
                
        if (p_sBranchCd.isEmpty()) p_sBranchCd = p_oApp.getBranchCode();
                
        p_nEditMode = EditMode.UNKNOWN;
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
    
   
    public boolean SearchRecord(String fsValue, boolean fbByCode) throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        p_sMessage = "";
        
        String lsSQL = getSQ_Redeem();
        
        if (p_bWithUI){
            JSONObject loJSON = showFXDialog.jsonSearch(
                                p_oApp, 
                                lsSQL, 
                                fsValue, 
                                "TransNox»Date", 
                                "sTransNox»dRedeemxx", 
                                "a.sTransNox»a.dRedeemxx", 
                                fbByCode ? 0 : 1);
            
            if (loJSON != null) 
                return OpenRecord((String) loJSON.get("sTransNox"));
            else {
                p_sMessage = "No record selected.";
                return false;
            }
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "sTransNox = " + SQLUtil.toSQL(fsValue));   
        else {
            lsSQL = MiscUtil.addCondition(lsSQL, "sTransNox LIKE " + SQLUtil.toSQL(fsValue + "%")); 
            lsSQL += " LIMIT 1";
        }
        
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        if (!loRS.next()){
            MiscUtil.close(loRS);
            p_sMessage = "No transaction found for the givern criteria.";
            return false;
        }
        
        lsSQL = loRS.getString("sTransNox");
        MiscUtil.close(loRS);
        
        return OpenRecord(lsSQL);
    }
    
    public boolean OpenRecord(String fsValue) throws SQLException{
        p_nEditMode = EditMode.UNKNOWN;
        
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        p_sMessage = "";
        
        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();
        
        //open master
        lsSQL = MiscUtil.addCondition(getSQ_Redeem(), "sTransNox= " + SQLUtil.toSQL(fsValue));
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
            if(p_oRecord.getString("sUserIDxx").length() == 10){
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
                   " AND a.sAcctNmbr = " + SQLUtil.toSQL(p_oRecord.getString("sUserIDxx"));
            }else{
                lsSQL = "SELECT" +
                   " IFNULL(b.sCompnyNm, CONCAT(b.sLastName, ', ', b.sFrstName, ' ', IFNULL(b.sSuffixNm, ''), ' ', b.sMiddName)) xCompnyNm" +
                   ", c.sBranchNm " +
                   ", b.sMobileNo " +
                   ", TRIM(CONCAT(IFNULL(b.sHouseNox,''),' ',IFNULL(b.sAddressx,''),' ',IFNULL(f.sBrgyName,''),', ',IFNULL(d.sTownName,''),', ',IFNULL(e.sProvName,''))) sAddressx " +
                " FROM Client_Master b" +
                   "    LEFT JOIN Employee_Master001 a " +
                   "        ON b.sClientID = a.sEmployID " +
                   "    LEFT JOIN TownCity d" +
                   "        ON b.sTownIDxx = d.sTownIDxx " +
                   "    LEFT JOIN Province e" +
                   "        ON d.sProvIDxx = e.sProvIDxx " +
                   "    LEFT JOIN Barangay f" +
                   "        ON b.sTownIDxx = f.sTownIDxx " +
                        "        AND b.sBrgyIDxx = f.sBrgyIDxx " +
                " , Branch c" +
                " WHERE a.sBranchCd = c.sBranchCd AND b.sClientID = " + SQLUtil.toSQL(p_oRecord.getString("sUserIDxx"));
            }
             loRS = p_oApp.executeQuery(lsSQL); 
             System.out.println(lsSQL);
             if(loRS.next()){
//                 System.out.println(p_oRecord.getString("cGroupNox"));
                p_oRecord.updateString("sCompanyNm", loRS.getString("xCompnyNm"));
                p_oRecord.updateString("sBranchNme", loRS.getString("sBranchNm"));
                p_oRecord.updateRow();
             }
             MiscUtil.close(loRS);
        }
    } 
  
    
    public int getItemCount() throws SQLException{
        if(p_oRecord == null) return 0;
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
    
    public Object getMaster(int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        
        p_oRecord.first();
        return p_oRecord.getObject(fnIndex);
    }
    
    public Object getMaster(String fsIndex) throws SQLException{
        return getMaster(getColumnIndex(fsIndex));
    }
    
    public void setMaster(int fnIndex, Object foValue) throws SQLException{
        p_oRecord.first();
        switch (fnIndex){
            case 2://sPanaloDs
                p_oRecord.updateString(fnIndex, ((String) foValue).trim());
                p_oRecord.updateRow();

                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oRecord.getString(fnIndex));
                break;
        }
    }
    
    public void setMaster(String fsIndex, Object foValue) throws SQLException{        
        setMaster(getColumnIndex(fsIndex), foValue);
    }

    public void displayMasFields() throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW && p_nEditMode != EditMode.UPDATE) return;
        
        int lnRow = p_oRecord.getMetaData().getColumnCount();
        
        System.out.println("----------------------------------------");
        System.out.println("MASTER TABLE INFO");
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
        
        System.out.println("----------------------------------------");
        System.out.println("END: MASTER TABLE INFO");
        System.out.println("----------------------------------------");
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
    private int getColumnIndex(String fsValue) throws SQLException{
        int lnIndex = 0;
        int lnRow = p_oRecord.getMetaData().getColumnCount();
        
        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            if (fsValue.equals(p_oRecord.getMetaData().getColumnLabel(lnCtr))){
                lnIndex = lnCtr;
                break;
            }
        }
        
        return lnIndex;
    }
    
    private String getSQ_Record(){
        return "SELECT" +
                    "  a.sTransNOx" +
                    ", a.dRedeemxx" +	
                    ", a.sUserIDxx" +
                    ", sModified" +	
                    ", dModified" +
                " FROM " + MASTER_TABLE;
    }
   
    public String getSQ_Redeem(){
        String lsSQL;
        
        lsSQL = "SELECT " +
                    "  a.sTransNox sTransNox" +
                    ", a.dRedeemxx dRedeemxx" +	
                    ", a.sDeviceID sDeviceID" +
                    ", IFNULL(c.sPartsIDx, '') sItemCode" +	
                    ", IFNULL(a.nItemQtyx, 0) nItemQtyx" +
                    ", IFNULL(a.sRemarksx, '') sRemarksx" +
                    ", IFNULL(b.sClientID, '') sUserIDxx" +
                    ", '' sUserName " +
                    ", IFNULL(a.sApproved,'') sApproved" +
                    ", IFNULL(a.dApproved, '') dApproved" +	
                    ", a.cSendStat cSendStat" +
                    ", a.cTranStat cTranStat" +
                    ", a.sModified sModified" +	
                    ", a.dModified dModified" +
                    ", IFNULL(c.sDescript, '') sDescript" +
                " FROM " + MASTER_TABLE + " a " +
                    " LEFT JOIN Spareparts c " + 
                        " ON a.sItemCode = c.sPartsIDx ";
        return lsSQL;
    }
}
