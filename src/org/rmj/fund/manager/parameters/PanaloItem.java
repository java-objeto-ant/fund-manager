/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.rmj.fund.manager.parameters;

import com.sun.rowset.CachedRowSetImpl;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import javax.sql.RowSetMetaData;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.RecordStatus;
import org.rmj.appdriver.constants.UserRight;
import org.rmj.fund.manager.base.LMasDetTrans;


/**
 *
 * @author User
 */
public class PanaloItem {
    private final String MASTER_TABLE = "Panalo_Item";
    
    private final GRider p_oApp;
    private final boolean p_bWithParent;
    
    private String p_sBranchCd;
    private int p_nTranStat;
    private int p_nEditMode;

    private String p_sMessage;
    private boolean p_bWithUI = true;
    
    private LMasDetTrans p_oListener;
    
    private CachedRowSet p_oRecord;
    
    public PanaloItem(GRider foApp, String fsBranchCd, boolean fbWithParent){
        p_oApp = foApp;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;        
                
        if (p_sBranchCd.isEmpty()) p_sBranchCd = p_oApp.getBranchCode();
                
        p_nEditMode = EditMode.UNKNOWN;
    }
    
    public void setListener(LMasDetTrans foValue){
        p_oListener = foValue;
    }
    
    public void setTranStat(int fnValue){
        p_nTranStat = fnValue;
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
    
    public boolean NewRecord() throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        p_sMessage = "";
        
        initRecord();

        p_nEditMode = EditMode.ADDNEW;
        return true;
    }
    
    public boolean SaveRecord() throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        p_sMessage = "";
        
        if (p_nEditMode != EditMode.ADDNEW &&
            p_nEditMode != EditMode.UPDATE){
            p_sMessage = "Invalid edit mode detected.";
            return false;
        }
        
        if (!isEntryOK()) return false;
        
        p_oRecord.updateObject("sModified", p_oApp.getUserID());
        p_oRecord.updateObject("dModified", p_oApp.getServerDate());
        p_oRecord.updateRow();
        
        String lsSQL;
        
        if (p_nEditMode == EditMode.ADDNEW){           
            lsSQL = MiscUtil.rowset2SQL(p_oRecord, MASTER_TABLE, "sPanaloDs;sBarrcode;xDescript");
        } else {            
            lsSQL = MiscUtil.rowset2SQL(p_oRecord, 
                                        MASTER_TABLE, 
                                        "sPanaloDs;sBarrcode;xDescript", 
                                        "sItemIDxx = " + SQLUtil.toSQL(p_oRecord.getString("sItemIDxx")));
        }
        
        if (!lsSQL.isEmpty()){
            if (!p_bWithParent) p_oApp.beginTrans();
            
            if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
                if (!p_bWithParent) p_oApp.rollbackTrans();
                p_sMessage = p_oApp.getErrMsg() + ";" + p_oApp.getMessage();
                return false;
            }
             
            
            if (!p_bWithParent) p_oApp.commitTrans();
            
            return true;
        } else
            p_sMessage = "No record to update.";
        
        p_nEditMode = EditMode.UNKNOWN;
        return false;
    }
    
    public boolean ActivateRecord() throws SQLException{
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid edit mode.";
            return false;
        }
        
        p_oRecord.first();
        
        if ("1".equals(p_oRecord.getString("cRecdStat"))){
            p_sMessage = "Record is already activated..";
            return false;
        }
        
        String lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                            "  cRecdStat = '1'" +
                            ", sModified = " + SQLUtil.toSQL(p_oApp.getUserID()) +
                            ", dModified = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                        " WHERE sItemIDxx = " + SQLUtil.toSQL(p_oRecord.getString("sItemIDxx"));

        if (!p_bWithParent) p_oApp.beginTrans();
        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
            if (!p_bWithParent) p_oApp.rollbackTrans();
            p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
            return false;
        }
        if (!p_bWithParent) p_oApp.commitTrans();
        
        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }
    
    public boolean DeactivateRecord() throws SQLException{
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid edit mode.";
            return false;
        }
        
        p_oRecord.first();
        
        if ("0".equals(p_oRecord.getString("cRecdStat"))){
            p_sMessage = "Record is already deactivated..";
            return false;
        }
        
        String lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                            "  cRecdStat = '0'" +
                            ", sModified = " + SQLUtil.toSQL(p_oApp.getUserID()) +
                            ", dModified = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                        " WHERE sItemIDxx = " + SQLUtil.toSQL(p_oRecord.getString("sItemIDxx"));

        if (!p_bWithParent) p_oApp.beginTrans();
        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
            if (!p_bWithParent) p_oApp.rollbackTrans();
            p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
            return false;
        }
        if (!p_bWithParent) p_oApp.commitTrans();
        
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
        
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sItemIDxx = " + SQLUtil.toSQL(fsValue));   
        else {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sDescript LIKE " + SQLUtil.toSQL(fsValue + "%")); 
        }
        System.out.println("slq = " +lsSQL);
        if (p_bWithUI){
            JSONObject loJSON = showFXDialog.jsonSearch(
                                p_oApp, 
                                lsSQL, 
                                fsValue, 
                                "ID»Description", 
                                "sItemIDxx»sDescript", 
                                "a.sItemIDxx»a.sDescript", 
                                fbByCode ? 0 : 1);
            
            if (loJSON != null) 
                return OpenRecord((String) loJSON.get("sItemIDxx"));
            else {
                p_sMessage = "No record selected.";
                return false;
            }
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sItemIDxx = " + SQLUtil.toSQL(fsValue));   
        else {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sDescript LIKE " + SQLUtil.toSQL(fsValue + "%")); 
            lsSQL += " LIMIT 1";
        }
        
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        if (!loRS.next()){
            MiscUtil.close(loRS);
            p_sMessage = "No transaction found for the givern criteria.";
            return false;
        }
        
        lsSQL = loRS.getString("sItemIDxx");
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
        lsSQL = MiscUtil.addCondition(getSQ_Record(), "sItemIDxx= " + SQLUtil.toSQL(fsValue));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oRecord = factory.createCachedRowSet();
        p_oRecord.populate(loRS);
        MiscUtil.close(loRS);
        
        p_nEditMode = EditMode.READY;
        return true;
    }
    
    public boolean searchPanalo(String fsValue, boolean fbByCode) throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW) return false;
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        if (fbByCode)
            if (fsValue.equals((String) getMaster("sPanaloCd"))) return true;
        else
            if (fsValue.equals((String) getMaster("sPanaloDs"))) return true;
        
           
        p_sMessage = "";
             
        
        JSONObject loJSON;
        String lsSQL = "SELECT * FROM Panalo_Info WHERE cRecdStat = '1'";
        String lsCondition = "";
        
        if(fbByCode){
            lsCondition = "sPanaloCd = " + SQLUtil.toSQL(fsValue);
        }else{
            lsCondition = "sPanaloDs LIKE " + SQLUtil.toSQL(fsValue + "%");
        }
        if (!lsCondition.isEmpty()) lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        
        if (p_bWithUI){
            loJSON = showFXDialog.jsonSearch(
                                p_oApp, 
                                lsSQL, 
                                fsValue, 
                                "Code»Description", 
                                "sPanaloCd»sPanaloDs", 
                                "sPanaloCd»sPanaloDs", 
                                fbByCode ? 0 : 1);
            
            if (loJSON != null){
                
                p_oRecord.updateObject("sPanaloCd", (String) loJSON.get("sPanaloCD"));
                p_oRecord.updateObject("sPanaloDs", (String) loJSON.get("sPanaloDs"));
                p_oRecord.updateRow();
                if (p_oListener != null) p_oListener.MasterRetreive(4, getMaster("sPanaloDs"));
                return true;
            }
            else {
                p_sMessage = "No record selected.";
                return false;
            }
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "sPanaloCd = " + SQLUtil.toSQL(fsValue));
        else
            lsSQL = MiscUtil.addCondition(lsSQL, "sPanaloDs LIKE " + SQLUtil.toSQL(fsValue + "%"));
        
        lsSQL += " LIMIT 1";
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        JSONArray loArray = MiscUtil.RS2JSON(loRS);
        MiscUtil.close(loRS);
        
        if (loArray.isEmpty()) return false;
        
        loJSON = (JSONObject) loArray.get(0);
        System.out.println(loJSON);
        System.out.println("sPanaloCD =" + (String) loJSON.get("sPanaloCD"));
        p_oRecord.updateObject("sPanaloCd", (String) loJSON.get("sPanaloCD"));
        p_oRecord.updateObject("sPanaloDs", (String) loJSON.get("sPanaloDs"));
        p_oRecord.updateRow();

        if (p_oListener != null){
            p_oListener.MasterRetreive(3, getMaster("sPanaloCd"));
            p_oListener.MasterRetreive(4, getMaster("sPanaloDs"));
        }
        
        return true;
    }
    
    public boolean searchSpareparts(String fsValue, boolean fbByCode) throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW) return false;
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        if (fbByCode)
            if (fsValue.equals((String) getMaster("sBarrcode")) && !fsValue.isEmpty()) return true;
        else
            if (fsValue.equals((String) getMaster("xDescript")) && !fsValue.isEmpty()) return true;
        
        boolean fsByCode;
        p_sMessage = "";
             
        
        JSONObject loJSON;
        String lsSQL = "SELECT * FROM Spareparts";
        String lsCondition = "";
        
//        if(fbByCode){
//            if(lbByCode){
//                lsCondition = "sBarrcode = " + SQLUtil.toSQL(fsValue);
//            }else{
//                lsCondition = "sBarrcode LIKE " + SQLUtil.toSQL(fsValue + "%");
//            }
//        }else{
//            if(lbByCode){
//                lsCondition = "sDescript = " + SQLUtil.toSQL(fsValue);
//            }else{
//                lsCondition = "sDescript LIKE " + SQLUtil.toSQL(fsValue + "%");
//            }
//        }

        if (p_bWithUI){
            loJSON = showFXDialog.jsonSearch(
                                p_oApp, 
                                lsSQL, 
                                fsValue, 
                                "Parts ID»Barrcode»Description", 
                                "sPartsIDx»sBarrcode»sDescript", 
                                "sPartsIDx»sBarrcode»sDescript", 
                                fbByCode ? 1 : 2);
            
            if (loJSON != null){
                
                p_oRecord.updateObject("sBarrcode", (String) loJSON.get("sBarrcode"));
                p_oRecord.updateObject("sPartsIDx", (String) loJSON.get("sPartsIDx"));
                p_oRecord.updateObject("xDescript", (String) loJSON.get("sDescript"));
                p_oRecord.updateRow();
                if (p_oListener != null) 
                    p_oListener.MasterRetreive(5, getMaster("sBarrcode"));
                    p_oListener.MasterRetreive(6, getMaster("sPartsIDx"));
                    p_oListener.MasterRetreive(7, getMaster("xDescript"));
                    
                return true;
            }
            else {
                p_sMessage = "No record selected.";
                return false;
            }
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "sBarrcode = " + SQLUtil.toSQL(fsValue));
        else
            lsSQL = MiscUtil.addCondition(lsSQL, "sDescript LIKE " + SQLUtil.toSQL(fsValue + "%"));
        
        lsSQL += " LIMIT 1";
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        JSONArray loArray = MiscUtil.RS2JSON(loRS);
        MiscUtil.close(loRS);
        
        if (loArray.isEmpty()) return false;
        
        loJSON = (JSONObject) loArray.get(0);
        System.out.println(loJSON);
        p_oRecord.updateObject("sBarrcode", (String) loJSON.get("sBarrcode"));
        p_oRecord.updateObject("sPartsIDx", (String) loJSON.get("sPartsIDx"));
        p_oRecord.updateObject("xDescript", (String) loJSON.get("sDescript"));
        p_oRecord.updateRow();

        if (p_oListener != null){
            p_oListener.MasterRetreive(5, getMaster("sBarrcode"));
            p_oListener.MasterRetreive(6, getMaster("sPartsIDx"));
            p_oListener.MasterRetreive(7, getMaster("xDescript"));
        }
        
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
            case 3://sPanaloDs
                searchPanalo((String)foValue,  true);
                break;
            case 5://sBarrcode
                searchSpareparts((String)foValue,true);
            case 7://xDescript
                searchSpareparts((String)foValue,false);
                break;
            default:
                p_oRecord.updateString(fnIndex, ((String) foValue).trim());
                p_oRecord.updateRow();
                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oRecord.getString(fnIndex));
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
    
    private boolean isEntryOK() throws SQLException{
        p_oRecord.first();
        
        if (p_oRecord.getString("sDesCript").isEmpty()){
            p_sMessage = "Panalo item description must not be empty.";
            return false;
        }
        
        return true;
    }
    
    private void initRecord() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(10);

        meta.setColumnName(1, "sItemIDxx");
        meta.setColumnLabel(1, "sItemIDxx");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 4);
        
        meta.setColumnName(2, "sDescript");
        meta.setColumnLabel(2, "sDescript");
        meta.setColumnType(2, Types.VARCHAR);
        meta.setColumnDisplaySize(2, 128);
        
        meta.setColumnName(3, "sPanaloCd");
        meta.setColumnLabel(3, "sPanaloCd");
        meta.setColumnType(3, Types.VARCHAR);
        meta.setColumnDisplaySize(3, 4);
        
        meta.setColumnName(4, "sPanaloDs");
        meta.setColumnLabel(4, "sPanaloDs");
        meta.setColumnType(4, Types.VARCHAR);
        meta.setColumnDisplaySize(4, 96);
        
        meta.setColumnName(5, "sBarrcode");
        meta.setColumnLabel(5, "sBarrcode");
        meta.setColumnType(5, Types.VARCHAR);
        meta.setColumnDisplaySize(5, 20);
        
        meta.setColumnName(6, "sPartsIDx");
        meta.setColumnLabel(6, "sPartsIDx");
        meta.setColumnType(6, Types.VARCHAR);
        meta.setColumnDisplaySize(5, 12);
        
        meta.setColumnName(7, "xDescript");
        meta.setColumnLabel(7, "xDescript");
        meta.setColumnType(7, Types.VARCHAR);
        meta.setColumnDisplaySize(7, 50);
        
        meta.setColumnName(8, "cRecdStat");
        meta.setColumnLabel(8, "cRecdStat");
        meta.setColumnType(8, Types.VARCHAR);
        meta.setColumnDisplaySize(8, 1);
        
        meta.setColumnName(9, "sModified");
        meta.setColumnLabel(9, "sModified");
        meta.setColumnType(9, Types.VARCHAR);
        meta.setColumnDisplaySize(9, 12);
        
        meta.setColumnName(10, "dModified");
        meta.setColumnLabel(10, "dModified");
        meta.setColumnType(10, Types.DATE);
        
        p_oRecord = new CachedRowSetImpl();
        p_oRecord.setMetaData(meta);
        
        p_oRecord.last();
        p_oRecord.moveToInsertRow();
        
        MiscUtil.initRowSet(p_oRecord);    
        
        p_oRecord.updateString("sItemIDxx", MiscUtil.getNextCode(MASTER_TABLE, "sItemIDxx", true, p_oApp.getConnection(), p_sBranchCd));
        p_oRecord.updateString("cRecdStat", RecordStatus.INACTIVE);
        
        p_oRecord.insertRow();
        p_oRecord.moveToCurrentRow();
    }
    
    private String getSQ_Record(){
        String lsStat = String.valueOf(p_nTranStat);
        String lsCondition = "";
        if (lsStat.length() > 1){
            for (int lnCtr = 0; lnCtr <= lsStat.length()-1; lnCtr++){
                lsCondition += ", " + SQLUtil.toSQL(Character.toString(lsStat.charAt(lnCtr)));
            }
            
            lsCondition = " a.cRecdStat IN (" + lsCondition.substring(2) + ")";
        } else{  
            lsCondition = " a.cRecdStat = " + lsStat;
            
        }
        return "SELECT" +
                    "  IFNULL(a.sItemIDxx,'') sItemIDxx" +
                    ", IFNULL(a.sDescript,'') sDescript" +	
                    ", IFNULL(a.sPanaloCd,'') sPanaloCd" +
                    ", IFNULL(b.sPanaloDs,'') sPanaloDs" +
                    ", IFNULL(c.sBarrcode,'') sBarrcode" +
                    ", IFNULL(a.sPartsIDx,'') sPartsIDx" +	
                    ", IFNULL(c.sDescript,'') xDescript" +	
                    ", IFNULL(a.cRecdStat,'0') cRecdStat" +
                    ", IFNULL(a.sModified,'') sModified" +
                    ", IFNULL(a.dModified,'') dModified" +
                " FROM " + MASTER_TABLE + " a" + 
                "   LEFT JOIN Panalo_Info b " + 
                "       ON a.sPanaloCd = b.sPanaloCd " +
                "   LEFT JOIN Spareparts c " + 
                "       ON a.sPartsIDx = c.sPartsIDx " + 
                " WHERE " + lsCondition;
    }
}
