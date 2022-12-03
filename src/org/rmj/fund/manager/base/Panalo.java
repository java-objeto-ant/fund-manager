/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.rmj.fund.manager.base;

import com.sun.rowset.CachedRowSetImpl;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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
import org.rmj.appdriver.constants.RecordStatus;

/**
 *
 * @author User
 */
public class Panalo {
   private final String MASTER_TABLE = "Guanzon_Apps_Panalo";
   private final String DETAIL_TABLE = "Guanzon_Apps_Panalo";
    
    private final GRider p_oApp;
    private final boolean p_bWithParent;
    
    private String p_sBranchCd;
    private int p_nEditMode;

    private String p_sMessage;
    private boolean p_bWithUI = true;
    
    private LMasDetTrans p_oListener;
    
    private CachedRowSet p_oRecord;
    private CachedRowSet p_oRedeem;
    
    public Panalo(GRider foApp, String fsBranchCd, boolean fbWithParent){
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
   
    public boolean PostRecord() throws SQLException{
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid edit mode.";
            return false;
        }
        
        p_oRecord.first();
        
        if ("1".equals(p_oRecord.getString("cRecdStat"))){
            p_sMessage = "Record is active.";
            return false;
        }
        
        String lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                            "  cRecdStat = '1'" +
                            ", sModified = " + SQLUtil.toSQL(p_oApp.getUserID()) +
                            ", dModified = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                        " WHERE sPanaloCD = " + SQLUtil.toSQL(p_oRecord.getString("sPanaloCD"));

        if (!p_bWithParent) p_oApp.beginTrans();
        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
            if (!p_bWithParent) p_oApp.rollbackTrans();
            p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
            return false;
        }
        if (!p_bWithParent) p_oApp.commitTrans();
        
        if (p_oListener != null) p_oListener.MasterRetreive(6, "1");
        
        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }
    
    public boolean VoidRecord() throws SQLException{
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid edit mode.";
            return false;
        }
        
        p_oRecord.first();
        
        if ("0".equals(p_oRecord.getString("cRecdStat"))){
            p_sMessage = "Record is inactive.";
            return false;
        }
        
        String lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                            "  cRecdStat = '0'" +
                            ", sModified = " + SQLUtil.toSQL(p_oApp.getUserID()) +
                            ", dModified = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                        " WHERE sPanaloCD = " + SQLUtil.toSQL(p_oRecord.getString("sPanaloCD"));

        if (!p_bWithParent) p_oApp.beginTrans();
        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
            if (!p_bWithParent) p_oApp.rollbackTrans();
            p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
            return false;
        }
        if (!p_bWithParent) p_oApp.commitTrans();
        
        if (p_oListener != null) p_oListener.MasterRetreive(6, "0");
        
        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }
    
    public boolean SearchRecord(String fsValue, boolean fbByCode) throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        p_sMessage = "";
        
        String lsSQL = getSQ_Record();
        
        if (p_bWithUI){
            JSONObject loJSON = showFXDialog.jsonSearch(
                                p_oApp, 
                                lsSQL, 
                                fsValue, 
                                "Transaction No»Description", 
                                "sTransNox»sDescript", 
                                "sTransNox»sPanaloDs", 
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
            lsSQL = MiscUtil.addCondition(lsSQL, "sUserName LIKE " + SQLUtil.toSQL(fsValue + "%")); 
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
    public boolean LoadList(String fsTransNox) throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        p_sMessage = "";
        
        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();
        
        //open master
//        lsSQL = MiscUtil.addCondition(getSQ_Record(), "sTransNox= " + SQLUtil.toSQL(fsTransNox));
        loRS = p_oApp.executeQuery(getSQ_Record());
        p_oRecord = factory.createCachedRowSet();
        p_oRecord.populate(loRS);
        MiscUtil.close(loRS);
        
        p_nEditMode = EditMode.READY;
        return true;
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
        lsSQL = MiscUtil.addCondition(getSQ_Record(), "sTransNox= " + SQLUtil.toSQL(fsValue));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oRecord = factory.createCachedRowSet();
        p_oRecord.populate(loRS);
        MiscUtil.close(loRS);
        
        p_nEditMode = EditMode.READY;
        return true;
    }
    
    public boolean UpdateRecord() throws SQLException{
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid edit mode.";
            return false;
        }
        
        p_nEditMode = EditMode.UPDATE;
        return true;
    }
    
    public int getItemCount() throws SQLException{
        p_oRecord.last();
        return p_oRecord.getRow();
    }
    public Object getMaster(int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        
        p_oRecord.first();
        return p_oRecord.getObject(fnIndex);
    }
    
    public Object getMaster(String fsIndex) throws SQLException{
        return getMaster(getColumnIndex(p_oRecord, fsIndex));
    }
    
    public void setMaster(int fnIndex, Object foValue) throws SQLException{
        
                   
        p_oRecord.first();
        switch (fnIndex){
            case 2://dTransact
            case 11://dExpiryDt
            case 13://dRedeemxx
                if (foValue instanceof Date){
                    p_oRecord.updateDate(fnIndex, SQLUtil.toDate((Date) foValue));
                } else
                    p_oRecord.updateDate(fnIndex, SQLUtil.toDate(p_oApp.getServerDate()));
                
                p_oRecord.updateRow();
                
                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oRecord.getString(fnIndex));
                break;
            case 7://nAmountxx
                if (foValue instanceof Double)
                    p_oRecord.updateDouble(fnIndex, (double) foValue);
                else 
                    p_oRecord.updateDouble(fnIndex, 0.000);
                
                
                p_oRecord.updateRow();
                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oRecord.getString(fnIndex));
                break;
            case 9://nItemQtyx
            case 10://nRedeemxx
                if (foValue instanceof Integer)
                    p_oRecord.updateInt(fnIndex, (int) foValue);
                else 
                    p_oRecord.updateInt(fnIndex, 0);
                
                p_oRecord.updateRow();
                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oRecord.getString(fnIndex));
                break;
            case 3://sPanaloCD
            case 4://sAcctNmbr
            case 5://sSourceCD
            case 6://sSourceno
            case 8://sItemCode
            case 12://sUserIDxx
            case 14://sDeviceID
            case 15://sReleased
                p_oRecord.updateString(fnIndex, ((String) foValue).trim());
                p_oRecord.updateRow();

                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oRecord.getString(fnIndex));
                break;
        }
    }
    
    public void setMaster(String fsIndex, Object foValue) throws SQLException{       
        setMaster(getColumnIndex(p_oRecord, fsIndex), foValue);
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
    
//    private int getColumnIndex(String fsValue) throws SQLException{
//        int lnIndex = 0;
//        int lnRow = p_oRecord.getMetaData().getColumnCount();
//        
//        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
//            if (fsValue.equals(p_oRecord.getMetaData().getColumnLabel(lnCtr))){
//                lnIndex = lnCtr;
//                break;
//            }
//        }
//        
//        return lnIndex;
//    }
//    
    
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
    
    private void initRecord() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(20);

        meta.setColumnName(1, "sTransNox");
        meta.setColumnLabel(1, "sTransNox");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 12);
        
        meta.setColumnName(2, "dTransact");
        meta.setColumnLabel(2, "dTransact");
        meta.setColumnType(2, Types.DATE);
        
        meta.setColumnName(3, "sPanaloCD");
        meta.setColumnLabel(3, "sPanaloCD");
        meta.setColumnType(3, Types.VARCHAR);
        meta.setColumnDisplaySize(3, 4);
        
        meta.setColumnName(4, "sAcctNmbr");
        meta.setColumnLabel(4, "sAcctNmbr");
        meta.setColumnType(4, Types.VARCHAR);
        meta.setColumnDisplaySize(4, 10);
        
        meta.setColumnName(5, "sSourceCD");
        meta.setColumnLabel(5, "sSourceCD");
        meta.setColumnType(5, Types.VARCHAR);
        meta.setColumnDisplaySize(5, 4);
        
        meta.setColumnName(6, "sSourceNo");
        meta.setColumnLabel(6, "sSourceNo");
        meta.setColumnType(6, Types.VARCHAR);
        meta.setColumnDisplaySize(6, 12);
        
        meta.setColumnName(7, "nAmountxx");
        meta.setColumnLabel(7, "nAmountxx");
        meta.setColumnType(7, Types.DECIMAL);
        
        meta.setColumnName(8, "sItemCode");
        meta.setColumnLabel(8, "sItemCode");
        meta.setColumnType(8, Types.VARCHAR);
        meta.setColumnDisplaySize(8, 20);
        
        meta.setColumnName(9, "nItemQtyx");
        meta.setColumnLabel(9, "nItemQtyx");
        meta.setColumnType(9, Types.INTEGER);
        
        meta.setColumnName(10, "nRedeemxx");
        meta.setColumnLabel(10, "nRedeemxx");
        meta.setColumnType(10, Types.INTEGER);
        
        meta.setColumnName(11, "dExpiryDt");
        meta.setColumnLabel(11, "dExpiryDt");
        meta.setColumnType(11, Types.DATE);
        
        meta.setColumnName(12, "sUserIDxx");
        meta.setColumnLabel(12, "sUserIDxx");
        meta.setColumnType(12, Types.VARCHAR);
        meta.setColumnDisplaySize(12, 12);
        
        meta.setColumnName(13, "sUserName");
        meta.setColumnLabel(13, "sUserName");
        meta.setColumnType(13, Types.VARCHAR);
        meta.setColumnDisplaySize(13, 64);
        
        meta.setColumnName(14, "dRedeemxx");
        meta.setColumnLabel(14, "dRedeemxx");
        meta.setColumnType(14, Types.DATE);
        
        meta.setColumnName(15, "sDeviceID");
        meta.setColumnLabel(15, "sDeviceID");
        meta.setColumnType(15, Types.VARCHAR);
        meta.setColumnDisplaySize(15, 32);
        
        meta.setColumnName(16, "sReleased");
        meta.setColumnLabel(16, "sReleased");
        meta.setColumnType(16, Types.VARCHAR);
        meta.setColumnDisplaySize(16, 12);
        
        meta.setColumnName(17, "cTranStat");
        meta.setColumnLabel(17, "cTranStat");
        meta.setColumnType(17, Types.VARCHAR);
        meta.setColumnDisplaySize(17, 1);
        
        meta.setColumnName(18, "sModified");
        meta.setColumnLabel(18, "sModified");
        meta.setColumnType(18, Types.VARCHAR);
        meta.setColumnDisplaySize(18, 12);
        
        meta.setColumnName(19, "dModified");
        meta.setColumnLabel(19, "dModified");
        meta.setColumnType(19, Types.DATE);
        
        meta.setColumnName(20, "sDescript");
        meta.setColumnLabel(20, "sDescript");
        meta.setColumnType(20, Types.VARCHAR);
        meta.setColumnDisplaySize(20, 64);
        
        p_oRecord = new CachedRowSetImpl();
        p_oRecord.setMetaData(meta);
        
        p_oRecord.last();
        p_oRecord.moveToInsertRow();
        
        MiscUtil.initRowSet(p_oRecord);    
        
        p_oRecord.updateObject("sTransNox", MiscUtil.getNextCode(MASTER_TABLE, "sTransNox", false, p_oApp.getConnection(), ""));
        p_oRecord.updateObject("cRecdStat", RecordStatus.ACTIVE);
        p_oRecord.updateObject("dTransact", p_oApp.getServerDate());
        
        p_oRecord.insertRow();
        p_oRecord.moveToCurrentRow();
    }
    
    private String getSQ_Record(){
        return "SELECT" +
                    "  a.sTransNox" +
                    ", a.dTransact" +	
                    ", a.sPanaloCD" +
                    ", c.sAcctNmbr" +
                    ", a.sSourceCD" +	
                    ", a.sSourceNo" +
                    ", a.nAmountxx" +
                    ", d.sPartsIDx sItemCode" +	
                    ", a.nItemQtyx" +
                    ", a.nRedeemxx" +
                    ", a.dExpiryDt" +	
                    ", b.sUserIDxx" +
                    ", b.sUserName" +
                    ", a.dRedeemxx" +
                    ", a.sDeviceID" +	
                    ", a.sReleased" +
                    ", a.cTranStat" +
                    ", a.sModified" +	
                    ", a.dModified" +
                    ", d.sDescript" +
                " FROM " + MASTER_TABLE + " a " +
                "   LEFT JOIN App_User_Master b " + 
                "       ON a.sUserIDxx = b.sUserIDxx "+
                "   LEFT JOIN MC_AR_Master c " + 
                "       ON a.sAcctNmbr = c.sAcctNmbr "+
                "   LEFT JOIN Spareparts d " + 
                "       ON a.sItemCode = d.sPartsIDx "+
                "   LEFT JOIN Guanzon_Panalo_Redemption e " + 
                "       ON a.sTransNox = e.sReferNox ";
    }
}
