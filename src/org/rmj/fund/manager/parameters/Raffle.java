/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.rmj.fund.manager.parameters;

import com.sun.rowset.CachedRowSetImpl;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.RowSetMetaData;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import static org.rmj.appdriver.MiscUtil.rowset2SQL;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.StringUtil;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.RecordStatus;
import org.rmj.fund.manager.base.LMasDetTrans;

/**
 *
 * @author User
 */
public class Raffle {
    private final String MASTER_TABLE = "ILMJ_Master";
    private final String DETAIL_TABLE = "ILMJ_Detail";
    
    private final GRider p_oApp;
    private final boolean p_bWithParent;
    
    private String p_sBranchCd;
    private int p_nTranStat;
    private int p_nEditMode;

    private String p_sMessage;
    private boolean p_bWithUI = true;
    
    private LMasDetTrans p_oListener;
    
    private CachedRowSet p_oMaster;
    private CachedRowSet p_oDetail;
    
    public Raffle(GRider foApp, String fsBranchCd, boolean fbWithParent){
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
        
        initMaster();
        initDetail();
//        try {
//            addDetail((String) getMaster("sTransNox").toString(), true);
//        } catch (ParseException ex) {
//            Logger.getLogger(Raffle.class.getName()).log(Level.SEVERE, null, ex);
//        }
        System.out.println("sTransNox = " + getMaster("sTransNox").toString());
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
        
        p_oMaster.updateObject("sModified", p_oApp.getUserID());
        p_oMaster.updateObject("dModified", p_oApp.getServerDate());
        p_oMaster.updateRow();
        
        
        String lsSQL;
        
        int lnCtr;
        if (p_nEditMode == EditMode.ADDNEW){    
            
            if (!p_bWithParent) p_oApp.beginTrans();
            String lsTransNox = getMaster("sTransNox").toString();
            lsSQL = rowset2SQL(p_oMaster, MASTER_TABLE, "");
            if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
                if (!p_bWithParent) p_oApp.rollbackTrans();
                p_sMessage = p_oApp.getErrMsg() + ";" + p_oApp.getMessage();
                return false;
            }
            lnCtr = 1;
            p_oDetail.beforeFirst();
            while (p_oDetail.next()){
                lsSQL = MiscUtil.rowset2SQL(p_oDetail, DETAIL_TABLE,   "sPanaloDs;sDescript;");
                               
                if (p_oApp.executeQuery(lsSQL, "Incentive_Detail", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                    if (!p_bWithParent) p_oApp.rollbackTrans();
                    p_sMessage = p_oApp.getMessage() + " ; " + p_oApp.getErrMsg();
                    return false;
                }
                
                lnCtr++;
            }
            
            if (!p_bWithParent) p_oApp.commitTrans();
        } else {            
             if (!p_bWithParent) p_oApp.beginTrans();
            String lsTransNox = (String) getMaster("sTransNox");
            lsSQL = rowset2SQL(p_oMaster, 
                                MASTER_TABLE, 
                                "", 
                                "sTransNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox")));
             if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
                if (!p_bWithParent) p_oApp.rollbackTrans();
                p_sMessage = p_oApp.getErrMsg() + ";" + p_oApp.getMessage();
                return false;
            }
             lnCtr = 1;
            p_oDetail.beforeFirst();
            while (p_oDetail.next()){
                lsSQL = MiscUtil.rowset2SQL(p_oDetail, 
                                            DETAIL_TABLE,
                                            "sPanaloDs;sDescript;", 
                                            "sTransNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox")));
                               
                if (p_oApp.executeQuery(lsSQL, "Incentive_Detail", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                    if (!p_bWithParent) p_oApp.rollbackTrans();
                    p_sMessage = p_oApp.getMessage() + " ; " + p_oApp.getErrMsg();
                    return false;
                }
                
                lnCtr++;
            }
            if (!p_bWithParent) p_oApp.commitTrans();
        }
//        
//        if (!lsSQL.isEmpty()){
//            if (!p_bWithParent) p_oApp.beginTrans();
//            System.out.println(p_sBranchCd);
//            if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
//                if (!p_bWithParent) p_oApp.rollbackTrans();
//                p_sMessage = p_oApp.getErrMsg() + ";" + p_oApp.getMessage();
//                return false;
//            }
//            
//            if (p_nEditMode == EditMode.ADDNEW){           
//                lsSQL = rowset2SQL(p_oDetail, DETAIL_TABLE, "sPanaloDs;sDescript;");
//            } else {            
//                lsSQL = rowset2SQL(p_oDetail, 
//                                            DETAIL_TABLE, 
//                                            "sPanaloDs;sDescript;", 
//                                            "sTransNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox")));
//            }
//            if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
//                if (!p_bWithParent) p_oApp.rollbackTrans();
//                p_sMessage = p_oApp.getErrMsg() + ";" + p_oApp.getMessage();
//                return false;
//            }
//            if (!p_bWithParent) p_oApp.commitTrans();
//            
        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }
    
    public boolean ActivateRecord() throws SQLException{
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid edit mode.";
            return false;
        }
        
        p_oMaster.first();
        
        if ("1".equals(p_oMaster.getString("cTranStat"))){
            p_sMessage = "Record is already activated..";
            return false;
        }
        
        String lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                            "  cTranStat = '1'" +
                            ", sModified = " + SQLUtil.toSQL(p_oApp.getUserID()) +
                            ", dModified = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                        " WHERE sTransNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox"));

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
        
        p_oMaster.first();
        
        if ("0".equals(p_oMaster.getString("cTranStat"))){
            p_sMessage = "Record is already deactivated..";
            return false;
        }
        
        String lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                            "  cTranStat = '0'" +
                            ", sModified = " + SQLUtil.toSQL(p_oApp.getUserID()) +
                            ", dModified = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                        " WHERE sTransNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox"));

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
            lsSQL = MiscUtil.addCondition(lsSQL, "sTransNox = " + SQLUtil.toSQL(fsValue));   
        else {
            lsSQL = MiscUtil.addCondition(lsSQL, "dRaffleDt LIKE " + SQLUtil.toSQL(fsValue + "%")); 
        }
        System.out.println("slq = " +lsSQL);
        if (p_bWithUI){
            JSONObject loJSON = showFXDialog.jsonSearch(
                                p_oApp, 
                                lsSQL, 
                                fsValue, 
                                "TansNox»Raffle Date", 
                                "sTransNox»dRaffleDt", 
                                "sTransNox»dRaffleDt", 
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
            lsSQL = MiscUtil.addCondition(lsSQL, "dRaffleDt LIKE " + SQLUtil.toSQL(fsValue + "%")); 
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
        lsSQL = MiscUtil.addCondition(getSQ_Record(), "sTransNox = " + SQLUtil.toSQL(fsValue));
        System.out.println(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_oMaster = factory.createCachedRowSet();
        p_oMaster.populate(loRS);
        MiscUtil.close(loRS);
       
        lsSQL = MiscUtil.addCondition(getSQ_Detail(), "a.sTransNox = " + SQLUtil.toSQL(fsValue));
        System.out.println(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDetail = factory.createCachedRowSet();
        p_oDetail.populate(loRS);
        MiscUtil.close(loRS);
        
        p_nEditMode = EditMode.READY;
        return  true;
    }
    
    public boolean LoadDetail(String fsValue, boolean fbByCode) throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        p_sMessage = "";    
        
        String lsSQL = "";
        
        if (fbByCode)
            lsSQL = getSQ_Detail() + " AND a.sTransNox = " + SQLUtil.toSQL(fsValue);
        else
            lsSQL = getSQ_Detail() + " AND a.sTransNox LIKE " + SQLUtil.toSQL(fsValue + "%");
        
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
    
        if (MiscUtil.RecordCount(loRS) == 0){
            MiscUtil.close(loRS);
            p_sMessage = "No record found for the given criteria.";
            return false;
        }
        
        RowSetFactory factory = RowSetProvider.newFactory();
        p_oDetail = factory.createCachedRowSet();
        p_oDetail.populate(loRS);
        MiscUtil.close(loRS);
        
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
        if (p_oDetail == null) return 0;
        
        p_oDetail.last();
        return p_oDetail.getRow();
    }
    public Object getMaster(int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        
        p_oMaster.first();
        return p_oMaster.getObject(fnIndex);
    }
    
    public Object getMaster(String fsIndex) throws SQLException{
        return getMaster(getColumnIndex(p_oMaster,fsIndex));
    }
    public Object getMaster(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        
        p_oMaster.absolute(fnRow);
        return p_oMaster.getObject(fnIndex);
    }
    
    public Object getMaster(int fnRow, String fsIndex) throws SQLException{
        return getMaster(fnRow, getColumnIndex(p_oMaster, fsIndex));
    }
    public void setMaster(String fsIndex, Object foValue) throws SQLException{        
        setMaster(getColumnIndex(p_oMaster,fsIndex), foValue);
    }
    public void setMaster(int fnIndex, Object foValue) throws SQLException{
        p_oMaster.first();
        switch (fnIndex){
            case 3://dRaffleDt
                System.out.println(fnIndex);
                if (foValue instanceof Date){
                    p_oMaster.updateDate(fnIndex, SQLUtil.toDate((Date) foValue));
                } else
                    p_oMaster.updateObject(fnIndex, foValue);
                
                p_oMaster.updateRow();
                
                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oMaster.getString(fnIndex));
                break;
            case 4://sRemarksx
                p_oMaster.updateString(fnIndex, ((String) foValue).trim());
                p_oMaster.updateRow();

                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oMaster.getString(fnIndex));
                break;
        }
    }
    
    
    public Object getDetail(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        if (getItemCount() == 0 || fnRow > getItemCount()) return null;
        
        p_oDetail.absolute(fnRow);
        return p_oDetail.getObject(fnIndex);
        
    }
    
    public Object getDetail(int fnRow, String fsIndex) throws SQLException{
        return getDetail(fnRow, getColumnIndex(p_oDetail, fsIndex));
    }
    
    public void setDetail(int fnRow,String fsIndex, Object foValue) throws SQLException{        
        setDetail(fnRow, getColumnIndex(p_oDetail,fsIndex), foValue);
    }
    public void setDetail(int fnRow,int fnIndex, Object foValue) throws SQLException{
         if (getItemCount()== 0 || fnRow > getItemCount()) return;
        
        p_oDetail.absolute(fnRow);
         switch (fnIndex){
            case 7://nAmountxx 
                p_oDetail.updateDouble(fnIndex, 0.00);
                
                if (StringUtil.isNumeric(String.valueOf(foValue))) 
                    p_oDetail.updateDouble(fnIndex, Double.parseDouble(foValue.toString()));
                
                p_oDetail.updateRow();   
                if (p_oListener != null) p_oListener.DetailRetreive(fnRow,fnIndex, p_oDetail.getString(fnIndex));
                break;
            case 8://nItemQtyx 
                p_oDetail.updateInt(fnIndex, 0);
                
                if (StringUtil.isNumeric(String.valueOf(foValue))) 
                    p_oDetail.updateInt(fnIndex, Integer.parseInt(foValue.toString()));
                
                p_oDetail.updateRow();   
                if (p_oListener != null) p_oListener.DetailRetreive(fnRow,fnIndex, p_oDetail.getString(fnIndex));
                break;
            default:
                p_oDetail.updateString(fnIndex, ((String) foValue).trim());
                p_oDetail.updateRow();
                if (p_oListener != null) p_oListener.DetailRetreive(fnRow,fnIndex, p_oDetail.getString(fnIndex));
        }
         
    }
    
    public boolean searchPanalo(int fnRow,String fsValue, boolean fbByCode) throws SQLException{
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
             
        System.out.println("row = " + fnRow);
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
                p_oDetail.absolute(fnRow);
                p_oDetail.updateObject("sPanaloCd", (String) loJSON.get("sPanaloCD"));
                p_oDetail.updateObject("sPanaloDs", (String) loJSON.get("sPanaloDs"));
                p_oDetail.updateRow();
                if (p_oListener != null) 
                    p_oListener.DetailRetreive(fnRow, 2, getDetail(fnRow,"sPanaloCd"));
                    p_oListener.DetailRetreive(fnRow, 3, getDetail(fnRow,"sPanaloDs"));
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
        
        p_oDetail.absolute(fnRow);
        p_oDetail.updateObject("sPanaloCd", (String) loJSON.get("sPanaloCD"));
        p_oDetail.updateObject("sPanaloDs", (String) loJSON.get("sPanaloDs"));
        p_oDetail.updateRow();

        if (p_oListener != null){
            p_oListener.DetailRetreive(fnRow, 2, getDetail(fnRow,"sPanaloCd"));
            p_oListener.DetailRetreive(fnRow, 3, getDetail(fnRow,"sPanaloDs"));
        }
        
        return true;
    }
    public boolean addDetail(String fsValue, boolean fbEmphasis) throws SQLException, ParseException{
        if(getItemCount()>0){
            
            p_oDetail.last();
            p_oDetail.moveToInsertRow();

            MiscUtil.initRowSet(p_oDetail);    

            p_oDetail.updateObject("sTransNox", getMaster("sTransNox"));
            p_oDetail.updateObject("sPanaloCd", "");
            p_oDetail.updateObject("sPanaloDs", "");
            p_oDetail.updateObject("sItemIDxx", "");
            p_oDetail.updateObject("sDescript", "");
            p_oDetail.updateObject("cWinnerxx", "");
            p_oDetail.updateObject("nAmountxx", 0.0);
            p_oDetail.updateObject("nItemQtyx", 0);


            p_oDetail.insertRow();
            p_oDetail.moveToCurrentRow();
        }
        return true;
    }
    
    public boolean delDescription(int fnRow) throws SQLException, ParseException{
        
        p_oDetail.absolute(fnRow);
        if (p_oDetail.getString("sPanaloCd").toString().isEmpty()) 
            return false;
        else {
            if (fnRow > getItemCount()) return false;
            
            p_oDetail.deleteRow();
        }
            
        return true;
    }
    public boolean searchItems(int fnRow,String fsValue, boolean fbByCode) throws SQLException{
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
        String lsSQL = "SELECT * FROM Panalo_Item WHERE cRecdStat = '1'";
        String lsCondition = "";
        p_oDetail.absolute(fnRow);
        if(p_oDetail.getString("sPanaloCd").isEmpty()){
            p_sMessage = "Please select panalo type first!";
            return false;
        }
        if(fbByCode){
            lsCondition = "sItemIDxx = " + SQLUtil.toSQL(fsValue) + " AND sPanaloCd = " + SQLUtil.toSQL(p_oDetail.getString("sPanaloCd"));
        }else{
            lsCondition = "sDescript LIKE " + SQLUtil.toSQL(fsValue + "%") + " AND sPanaloCd = " + SQLUtil.toSQL(p_oDetail.getString("sPanaloCd"));
        }
        
        if (!lsCondition.isEmpty()) lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        
        if (p_bWithUI){
            loJSON = showFXDialog.jsonSearch(
                                p_oApp, 
                                lsSQL, 
                                fsValue, 
                                "Item ID»Description", 
                                "sItemIDxx»sDescript", 
                                "sItemIDxx»sDescript", 
                                fbByCode ? 0 : 1);
            
            if (loJSON != null){
                p_oDetail.updateObject("sItemIDxx", (String) loJSON.get("sItemIDxx"));
                p_oDetail.updateObject("sDescript", (String) loJSON.get("sDescript"));
                p_oDetail.updateRow();
                if (p_oListener != null) 
                    p_oListener.DetailRetreive(fnRow, 4, getDetail(fnRow, "sItemIDxx"));
                    p_oListener.DetailRetreive(fnRow, 5, getDetail(fnRow, "sDescript"));
                return true;
            }
            else {
                p_sMessage = "No record selected.";
                return false;
            }
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "sItemIDxx = " + SQLUtil.toSQL(fsValue));
        else
            lsSQL = MiscUtil.addCondition(lsSQL, "sDescript LIKE " + SQLUtil.toSQL(fsValue + "%"));
        
        lsSQL += " LIMIT 1";
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        JSONArray loArray = MiscUtil.RS2JSON(loRS);
        MiscUtil.close(loRS);
        
        if (loArray.isEmpty()) return false;
        
        loJSON = (JSONObject) loArray.get(0);
        System.out.println(loJSON);
        System.out.println("sItemIDxx =" + (String) loJSON.get("sItemIDxx"));
        
        p_oDetail.updateObject("sItemIDxx", (String) loJSON.get("sItemIDxx"));
        p_oDetail.updateObject("sDescript", (String) loJSON.get("sDescript"));
        p_oDetail.updateRow();

        if (p_oListener != null){
            p_oListener.DetailRetreive(fnRow, 4, getDetail(fnRow, "sItemIDxx"));
            p_oListener.DetailRetreive(fnRow, 5, getDetail(fnRow, "sDescript"));
        }
        
        return true;
    }
    public boolean searchItemss(int fnRow, String fsValue, boolean fbByCode) throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW) return false;
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        if (fbByCode)
            if (fsValue.equals((String) getMaster("sItemIDxx")) && !fsValue.isEmpty()) return true;
        else
            if (fsValue.equals((String) getMaster("sDescript")) && !fsValue.isEmpty()) return true;
        
        boolean fsByCode;
        p_sMessage = "";
             
        
        JSONObject loJSON;
        String lsSQL = "SELECT * FROM Panalo_Item WHERE cRecdStat = '1'";
        String lsCondition = "";
        
//        if (fbByCode)
//            lsSQL = MiscUtil.addCondition(lsSQL, "sItemIDxx = " + SQLUtil.toSQL(fsValue));
//        else
//            lsSQL = MiscUtil.addCondition(lsSQL, "sDescript LIKE " + SQLUtil.toSQL(fsValue + "%"));
        System.out.println("lsSQL = " + lsSQL);
        if (p_bWithUI){
            loJSON = showFXDialog.jsonSearch(
                                p_oApp, 
                                lsSQL, 
                                fsValue, 
                                "Item ID»Description", 
                                "sItemIDxx»sDescript", 
                                "sItemIDxx»sDescript", 
                                fbByCode ? 1 : 2);
            
            if (loJSON != null){
                
                p_oDetail.absolute(fnRow);
                p_oDetail.updateObject("sItemIDxx", (String) loJSON.get("sItemIDxx"));
                p_oDetail.updateObject("sDescript", (String) loJSON.get("sDescript"));
                p_oDetail.updateRow();
                if (p_oListener != null) 
                    p_oListener.DetailRetreive(fnRow, 4, getMaster("sItemIDxx"));
                    p_oListener.DetailRetreive(fnRow, 5, getMaster("sDescript"));
                    
                return true;
            }
            else {
                p_sMessage = "No record selected.";
                return false;
            }
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "sItemIDxx = " + SQLUtil.toSQL(fsValue));
        else
            lsSQL = MiscUtil.addCondition(lsSQL, "sDescript LIKE " + SQLUtil.toSQL(fsValue + "%"));
        
        lsSQL += " LIMIT 1";
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        JSONArray loArray = MiscUtil.RS2JSON(loRS);
        MiscUtil.close(loRS);
        
        if (loArray.isEmpty()) return false;
        
        loJSON = (JSONObject) loArray.get(0);
        System.out.println(loJSON);
        p_oDetail.absolute(fnRow);
        p_oDetail.updateObject("sItemIDxx", (String) loJSON.get("sItemIDxx"));
        p_oDetail.updateObject("sDescript", (String) loJSON.get("sDescript"));
        p_oDetail.updateRow();

        if (p_oListener != null){
                p_oListener.DetailRetreive(fnRow, 4, getMaster("sItemIDxx"));
                p_oListener.DetailRetreive(fnRow, 5, getMaster("sDescript"));
        }
        
        return true;
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
    
    
    private boolean isEntryOK() throws SQLException{
        p_oMaster.first();
        
        if (p_oMaster.getString("dRaffleDt").isEmpty()){
            p_sMessage = "Raffle date must not be empty.";
            return false;
        }
        p_oDetail.beforeFirst();
        while(p_oDetail.next()){
            if(p_oDetail.getString("sPanaloCd").isEmpty()){
                p_sMessage = "Please select panalo rewards for raffle winners.";
                return false;
            }
            if(Double.parseDouble(p_oDetail.getString("sPanaloCd").toString())<=0.0){
                p_sMessage = "Rewards amount must not be zero.";
                return false;
            }
            if(Integer.parseInt(p_oDetail.getString("sPanaloCd").toString())<=0){
                p_sMessage = "Rewards quantity must not be zero.";
                return false;
            }
        }
        
        return true;
    }
    
    private void initMaster() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(8);

        meta.setColumnName(1, "sTransNox");
        meta.setColumnLabel(1, "sTransNox");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 12);
        
        meta.setColumnName(2, "dTransact");
        meta.setColumnLabel(2, "dTransact");
        meta.setColumnType(2, Types.DATE);
        
        meta.setColumnName(3, "dRaffleDt");
        meta.setColumnLabel(3, "dRaffleDt");
        meta.setColumnType(3, Types.TIMESTAMP);
        
        meta.setColumnName(4, "sRemarksx");
        meta.setColumnLabel(4, "sRemarksx");
        meta.setColumnType(4, Types.VARCHAR);
        meta.setColumnDisplaySize(4, 128);
        
        meta.setColumnName(5, "cTranStat");
        meta.setColumnLabel(5, "cTranStat");
        meta.setColumnType(5, Types.VARCHAR);
        meta.setColumnDisplaySize(5, 1);
        
        meta.setColumnName(6, "sModified");
        meta.setColumnLabel(6, "sModified");
        meta.setColumnType(6, Types.VARCHAR);
        meta.setColumnDisplaySize(6, 12);
        
        meta.setColumnName(7, "dModified");
        meta.setColumnLabel(7, "dModified");
        meta.setColumnType(7, Types.DATE);
        
        meta.setColumnName(8, "dTimeStmp");
        meta.setColumnLabel(8, "dTimeStmp");
        meta.setColumnType(8, Types.TIMESTAMP);
        
        p_oMaster = new CachedRowSetImpl();
        p_oMaster.setMetaData(meta);
        
        p_oMaster.last();
        p_oMaster.moveToInsertRow();
        
        MiscUtil.initRowSet(p_oMaster);    
        
        p_oMaster.updateString("sTransNox", MiscUtil.getNextCode(MASTER_TABLE, "sTransNox", true, p_oApp.getConnection(), p_oApp.getBranchCode()));
        p_oMaster.updateObject("cTranStat", 0);
        p_oMaster.updateObject("dTransact", p_oApp.getServerDate());
        
        p_oMaster.insertRow();
        p_oMaster.moveToCurrentRow();
    }
    private void initDetail() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(8);

        meta.setColumnName(1, "sTransNox");
        meta.setColumnLabel(1, "sTransNox");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 12);
        
        meta.setColumnName(2, "sPanaloCd");
        meta.setColumnLabel(2, "sPanaloCd");
        meta.setColumnType(2, Types.VARCHAR);
        meta.setColumnDisplaySize(2, 4);
        
        meta.setColumnName(3, "sPanaloDs");
        meta.setColumnLabel(3, "sPanaloDs");
        meta.setColumnType(3, Types.VARCHAR);
        meta.setColumnDisplaySize(3, 96);
        
        meta.setColumnName(4, "sItemIDxx");
        meta.setColumnLabel(4, "sItemIDxx");
        meta.setColumnType(4, Types.VARCHAR);
        meta.setColumnDisplaySize(4, 12);
        
        meta.setColumnName(5, "sDescript");
        meta.setColumnLabel(5, "sDescript");
        meta.setColumnType(5, Types.VARCHAR);
        meta.setColumnDisplaySize(5, 128);
        
        meta.setColumnName(6, "cWinnerxx");
        meta.setColumnLabel(6, "cWinnerxx");
        meta.setColumnType(6, Types.VARCHAR);
        meta.setColumnDisplaySize(6, 1);
        
        meta.setColumnName(7, "nAmountxx");
        meta.setColumnLabel(7, "nAmountxx");
        meta.setColumnType(7, Types.DOUBLE);
        
        meta.setColumnName(8, "nItemQtyx");
        meta.setColumnLabel(8, "nItemQtyx");
        meta.setColumnType(8, Types.SMALLINT);
        meta.setColumnDisplaySize(8, 1);
        
        p_oDetail = new CachedRowSetImpl();
        p_oDetail.setMetaData(meta);
        int lnRow;
        for(lnRow = 1;lnRow <= 4; lnRow++){
            p_oDetail.last();
            p_oDetail.moveToInsertRow();

            MiscUtil.initRowSet(p_oDetail);        
            p_oDetail.updateObject("sTransNox", (String) getMaster("sTransNox"));
            p_oDetail.updateObject("sPanaloCd", "");
            p_oDetail.updateObject("sPanaloDs", "");
            p_oDetail.updateObject("sItemIDxx", "");
            p_oDetail.updateObject("sDescript", "");
            p_oDetail.updateObject("cWinnerxx", String.valueOf(lnRow-1));
            p_oDetail.updateDouble("nAmountxx", 0.0);
            p_oDetail.updateInt("nItemQtyx", 0);

            p_oDetail.insertRow();
            p_oDetail.moveToCurrentRow();
        }
        
    }    
    private String getSQ_Record(){
        return "SELECT" +
                    " IFNULL(sTransNox,'')  sTransNox" +
                    ", IFNULL(dTransact,'') dTransact" +
                    ", IFNULL(dRaffleDt,'') dRaffleDt" +
                    ", IFNULL(sRemarksx,'') sRemarksx" +
                    ", IFNULL(cTranStat, 0) cTranStat" +
                    ", IFNULL(sModified,'') sModified" +
                    ", IFNULL(dModified,'') dModified" +
                    ", IFNULL(dTimeStmp,'') dTimeStmp" +
                " FROM " + MASTER_TABLE ;
    }
    private String getSQ_Detail(){
        String lsStat = String.valueOf(p_nTranStat);
        String lsCondition = "";
        if (lsStat.length() > 1){
            for (int lnCtr = 0; lnCtr <= lsStat.length()-1; lnCtr++){
                lsCondition += ", " + SQLUtil.toSQL(Character.toString(lsStat.charAt(lnCtr)));
            }
            
            lsCondition = " b.cRecdStat IN (" + lsCondition.substring(2) + ")" +
                    " AND c.cRecdStat IN (" + lsCondition.substring(2) + ")";
        } else{  
            lsCondition = " b.cRecdStat = " + lsStat + " AND b.cRecdStat = " + lsStat;
            
        }
        return "SELECT" +
                    "  IFNULL(a.sTransNox,'') sTransNox" +
                    ", IFNULL(a.sPanaloCd,'') sPanaloCd" +
                    ", IFNULL(b.sPanaloDs,'') sPanaloDs" +
                    ", IFNULL(a.sItemIDxx,'') sItemIDxx" +
                    ", IFNULL(c.sDescript,'') sDescript" +	
                    ", IFNULL(a.cWinnerxx,'0') cWinnerxx" +
                    ", IFNULL(a.nAmountxx,0.0) nAmountxx" +
                    ", IFNULL(a.nItemQtyx,0) nItemQtyx" +
                " FROM " + DETAIL_TABLE + " a" + 
                "   LEFT JOIN Panalo_Info b " + 
                "       ON a.sPanaloCd = b.sPanaloCd " +
                "   LEFT JOIN panalo_item c " + 
                "       ON a.sItemIDxx = c.sItemIDxx ";
    }
public void displayMasFields() throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW && p_nEditMode != EditMode.UPDATE) return;
        
        int lnRow = p_oMaster.getMetaData().getColumnCount();
        
        System.out.println("----------------------------------------");
        System.out.println("MASTER TABLE INFO");
        System.out.println("----------------------------------------");
        System.out.println("Total number of columns: " + lnRow);
        System.out.println("----------------------------------------");
        
        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            System.out.println("Column index: " + (lnCtr) + " --> Label: " + p_oMaster.getMetaData().getColumnLabel(lnCtr));
            if (p_oMaster.getMetaData().getColumnType(lnCtr) == Types.CHAR ||
                p_oMaster.getMetaData().getColumnType(lnCtr) == Types.VARCHAR){
                
                System.out.println("Column index: " + (lnCtr) + " --> Size: " + p_oMaster.getMetaData().getColumnDisplaySize(lnCtr));
            }
        }
        
        System.out.println("----------------------------------------");
        System.out.println("END: MASTER TABLE INFO");
        System.out.println("----------------------------------------");
        System.out.println("----------------------------------------");
        System.out.println("FIELD VALUES");
        System.out.println("----------------------------------------");
        System.out.println("----------------------------------------");
    }
    
}