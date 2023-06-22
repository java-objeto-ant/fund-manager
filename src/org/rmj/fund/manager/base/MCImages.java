/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.rmj.fund.manager.base;

import com.sun.rowset.CachedRowSetImpl;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.RowSetMetaData;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.appdriver.constants.EditMode;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author User
 */
public class MCImages {
    private final GRider p_oApp;
    private final boolean p_bWithParent;
    
    private final String MASTER_TABLE = "MC_Model_Images";
    
    private String p_sBranchCd;
    private int p_nEditMode;
    private int p_nTranStat;
    private String p_sMessage;
    private boolean p_bWithUI = true;
    private LMasDetTrans p_oListener;
    private CachedRowSet p_oImages;
    private JSONArray jsonImg;
    
    
    
    
    public MCImages(GRider foApp, String fsBranchCd, boolean fbWithParent){        
        p_oApp = foApp;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;        
                
        if (p_sBranchCd.isEmpty()) p_sBranchCd = p_oApp.getBranchCode();
        p_nTranStat = 0;
        jsonImg = new JSONArray();
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
    public boolean NewTransaction() throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        p_sMessage = "";
        
//        createImages();
        try { 
            addImage();
        } catch (org.json.simple.parser.ParseException ex) {
            Logger.getLogger(MCImages.class.getName()).log(Level.SEVERE, null, ex);
        }
        p_nEditMode = EditMode.ADDNEW;
        return true;
    }
    
    public int getImageCount() throws SQLException{
        if(p_oImages == null){
            return 0;
        }
        p_oImages.last();
        return p_oImages.getRow();
    }
    public int getItemCount() throws SQLException{
        if(jsonImg == null){
            return 0;
        }
        return jsonImg.size();
    }
    public Object getDetail(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        
        p_oImages.absolute(fnRow);
        return p_oImages.getObject(fnIndex);
    }
    
    public Object getDetail(int fnRow, String fsIndex) throws SQLException{
        return getDetail(fnRow, getColumnIndex(p_oImages, fsIndex));
    }
    
    public void setDetail(int fnRow, int fnIndex, Object foValue) throws SQLException{
        if (getImageCount()== 0 || fnRow > getImageCount()) return;
        
        p_oImages.absolute(fnRow);
        switch (fnIndex){
            case 3://sImageURL
                p_oImages.updateRow();
                p_oImages.updateString(fnIndex, (String) foValue);
                if (p_oListener != null) p_oListener.DetailRetreive(fnRow,fnIndex,getDetail(fnRow, fnIndex));
                break;
        }
        
    }
    
    public void setDetail(int fnRow, String fsIndex, Object foValue) throws SQLException{
        setDetail(fnRow, getColumnIndex(p_oImages, fsIndex), foValue);
    }
    
    private void createImages() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(10);

        meta.setColumnName(1, "sMCInvIDx");
        meta.setColumnLabel(1, "sMCInvIDx");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 9);

        meta.setColumnName(2, "nEntryNox");
        meta.setColumnLabel(2, "nEntryNox");
        meta.setColumnType(2, Types.INTEGER);
        meta.setColumnDisplaySize(2, 4);

        meta.setColumnName(3, "sImageURL");
        meta.setColumnLabel(3, "sImageURL");
        meta.setColumnType(3, Types.VARCHAR);
        meta.setColumnDisplaySize(3, 128);
        
        meta.setColumnName(4, "cPrimaryx");
        meta.setColumnLabel(4, "cPrimaryx");
        meta.setColumnType(4, Types.CHAR);
        meta.setColumnDisplaySize(4, 1);
        
        meta.setColumnName(5, "cRecdStat");
        meta.setColumnLabel(5, "cRecdStat");
        meta.setColumnType(5, Types.CHAR);
        meta.setColumnDisplaySize(5, 1);
        
        meta.setColumnName(6, "sModified");
        meta.setColumnLabel(6, "sModified");
        meta.setColumnType(6, Types.VARCHAR);
        meta.setColumnDisplaySize(6, 12);
        
        meta.setColumnName(7, "dModified");
        meta.setColumnLabel(7, "dModified");
        meta.setColumnType(7, Types.DATE);
        
        meta.setColumnName(8, "sBrandNme");
        meta.setColumnLabel(8, "sBrandNme");
        meta.setColumnType(8, Types.VARCHAR);
        meta.setColumnDisplaySize(8, 25);
        
        meta.setColumnName(9, "sModelNme");
        meta.setColumnLabel(9, "sModelNme");
        meta.setColumnType(9, Types.VARCHAR);
        meta.setColumnDisplaySize(9, 25);
        
        meta.setColumnName(10, "sColorNme");
        meta.setColumnLabel(10, "sColorNme");
        meta.setColumnType(10, Types.VARCHAR);
        meta.setColumnDisplaySize(8, 25);
        
        p_oImages = new CachedRowSetImpl();
        p_oImages.setMetaData(meta);
        
//        addImage();
    }
    
    // Getter method to retrieve an item from the JSON array
    
    public JSONObject getItem(int index) {
        if (index < 0 || index >= jsonImg.size()) {
            return null;
        }
        return (JSONObject) jsonImg.get(index);
    }

    // Setter method to update an item in the JSON array
    public void setItem(int index, JSONObject item) {
        if (index < 0 || index >= jsonImg.size()) {
            return;
        }
        jsonImg.set(index, item);
    }


    public boolean setImagePriority(int fnRow, boolean fbMoveUpxx) throws SQLException, org.json.simple.parser.ParseException{
//        String lsDescript = (String) getMaster("sImagesxx");
        
        JSONArray loArray;
        
        if (jsonImg.toString().isEmpty()) 
            return false;
        else {
            JSONParser loParser = new JSONParser();
            loArray = (JSONArray) loParser.parse(jsonImg.toString());
            
            if (fnRow > loArray.size()-1 || fnRow < 0) return false;
            
            if (fbMoveUpxx && fnRow == 0) return false;
            if (!fbMoveUpxx && fnRow == loArray.size()-1) return false;
            
            JSONObject loTemp = (JSONObject) loArray.get(fnRow);
            loArray.remove(fnRow);
            
            if (fbMoveUpxx)
                loArray.add(fnRow - 1, loTemp);
            else
                loArray.add(fnRow + 1, loTemp);
        }
            
        jsonImg = loArray;
        
        return true;
    }
    
    public boolean addImage() throws SQLException, org.json.simple.parser.ParseException{
//        String lsDescript = (String) getMaster("sImagesxx");
        
        JSONArray loArray ;
        
        JSONObject loJSON = new JSONObject();
        loJSON.put("sMCInvIDx", "");
        loJSON.put("nEntryNox", jsonImg.size() + 1);
        loJSON.put("sImageURL", "");
        loJSON.put("cPrimaryx", '0');
        loJSON.put("cRecdStat", "1");
        loJSON.put("sModified", "");
        loJSON.put("dModified", "");
        loJSON.put("sBrandNme", "");
        loJSON.put("sModelNme", "");
        loJSON.put("sColorNme", "");

        
        if (jsonImg.toString().isEmpty() || jsonImg == null) {
            loArray = new JSONArray();
            loArray.add(loJSON);
        } else {
            JSONParser loParser = new JSONParser();
            loArray = (JSONArray) loParser.parse(jsonImg.toString());
            loArray.add(loJSON);
        }
            
        jsonImg = loArray;
        System.out.println(jsonImg.size());
        return true;
    }
    
    public boolean delImage(int fnRow) throws SQLException, org.json.simple.parser.ParseException{
//        String lsDescript = (String) getMaster("sImagesxx");
        
        JSONArray loArray;
        
        if (jsonImg.toString().isEmpty()) 
            return false;
        else {
            JSONParser loParser = new JSONParser();
            loArray = (JSONArray) loParser.parse(jsonImg.toString());
            
            if (fnRow > loArray.size()) return false;
            
            loArray.remove(fnRow);
        }
            
//        setMaster("sImagesxx", loArray.toJSONString());
        jsonImg = loArray;
        return true;
    }
    public boolean searchItem(int fnRow, String fsValue, boolean fbByCode, boolean fbSearch) throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW) {
            p_sMessage = "This feature was for New Tranactions Only.";
            return false;
        }
                
        String lsCondition = "";
        if(fsValue == null){
            fsValue = "";
        }
//         if (!fsValue.trim().isEmpty()){
//            if (fbSearch)
//                lsCondition = "a.sModelCde LIKE " + SQLUtil.toSQL(fsValue + "%");
//            else
//                lsCondition = "a.sModelNme LIKE " + SQLUtil.toSQL(fsValue + "%");
//        }else{
//            lsCondition = "a.sModelNme LIKE " + SQLUtil.toSQL(fsValue + "%");
//             
//         }
//        

        if (fbSearch)
            lsCondition = "b.sModelCde LIKE " + SQLUtil.toSQL(fsValue + "%");
        else
            lsCondition = "b.sModelNme LIKE " + SQLUtil.toSQL(fsValue + "%");
        
        String lsSQL = "SELECT" +
                    "  a.sMCInvIDx " +
                    ", b.sModelCde " +
                    ", c.sBrandNme " +
                    ", b.sModelNme " +
                    ", d.sColorNme " +
                    ", a.nSelPrice " +
                " FROM MC_Inventory a" +
                    " LEFT JOIN Color d" +
                       " ON a.sColorIDx = d.sColorIDx" +
                    ", MC_Model b" +
                    ", Brand c" +
                " WHERE b.sBrandIDx = c.sBrandIDx" +
                    " AND a.sModelIDx = b.sModelIDx" +
                    " AND a.sBranchCd =  " + SQLUtil.toSQL(p_oApp.getBranchCode());
        
        if (!lsCondition.isEmpty())
            lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        
        ResultSet loRS;
        
        
        JSONObject jsonObj = (JSONObject) jsonImg.get(fnRow);
        if (!p_bWithUI){
            lsSQL += " LIMIT 1";
            
            loRS = p_oApp.executeQuery(lsSQL);
            System.out.println(lsSQL);
            if (loRS.next()){
                jsonObj.put("sMCInvIDx", loRS.getString("sMCInvIDx"));
                jsonObj.put("sModelCde", loRS.getString("sModelCde"));
                jsonObj.put("sBrandNme", loRS.getString("sBrandNme"));
                jsonObj.put("sModelNme", loRS.getString("sModelNme"));
                jsonObj.put("sColorNme", loRS.getString("sColorNme"));
                
                jsonImg.set(fnRow, jsonObj);
                if (p_oListener != null){
                    p_oListener.DetailRetreive(fnRow, 0, "");
//                    p_oListener.DetailRetreive(fnRow, getColumnIndex(p_oImages, "sBrandNme"), getDetail(fnRow,"sBrandNme"));
//                    p_oListener.DetailRetreive(fnRow, getColumnIndex(p_oImages, "sModelNme"), getDetail(fnRow,"sModelNme"));
//                    p_oListener.DetailRetreive(fnRow, getColumnIndex(p_oImages, "sColorNme"), getDetail(fnRow,"sColorNme"));
                }
                
                return true;
            } else return false;
        }
        
        System.out.println(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        
        JSONObject loJSON;
        
        loJSON = showFXDialog.jsonBrowse(
                    p_oApp, 
                    loRS, 
                    "Code»Brand»Model»Color", 
                    "sModelCde»sBrandNme»sModelNme»sColorNme");

        if (loJSON != null){
                jsonObj.put("sMCInvIDx", loRS.getString("sMCInvIDx"));
                jsonObj.put("sModelCde", loRS.getString("sModelCde"));
                jsonObj.put("sBrandNme", loRS.getString("sBrandNme"));
                jsonObj.put("sModelNme", loRS.getString("sModelNme"));
                jsonObj.put("sColorNme", loRS.getString("sColorNme"));
//                p_oImages.updateRow();
                
                if (p_oListener != null){
                    p_oListener.DetailRetreive(fnRow, 0, "");
//                    p_oListener.DetailRetreive(fnRow, getColumnIndex(p_oImages, "sMCInvIDx"), getDetail(fnRow,"sMCInvIDx"));
                }
                

            return true;
        }
        return false;
    }
    public boolean SaveTransaction() throws SQLException{
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
        
        
        int lnCtr;
        int lnRow;
        String lsSQL;
        convertJsonArrayToCachedRowSet(jsonImg);
        
        if (p_nEditMode == EditMode.ADDNEW){    
            if (!p_bWithParent) p_oApp.beginTrans();
            
            lnCtr = 1;
            p_oImages.beforeFirst();
            while (p_oImages.next()){
                
                if (!isEntryOK()) return false;
                String lsInvIDx = p_oImages.getString("sMCInvIDx");
                
            //set transaction number on records
                p_oImages.updateObject("sModified", p_oApp.getUserID());
                p_oImages.updateObject("dModified", p_oApp.getServerDate());
                p_oImages.updateObject("nEntryNox", lnCtr);
                p_oImages.updateRow();
                
                lsSQL = MiscUtil.rowset2SQL(p_oImages, MASTER_TABLE, "sBrandNme;sModelNme;sColorNme");
                               
                if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd,  lsInvIDx.substring(0, 4)) <= 0){
                    if (!p_bWithParent) p_oApp.rollbackTrans();
                    p_sMessage = p_oApp.getMessage() + " ; " + p_oApp.getErrMsg();
                    return false;
                }
                
                lnCtr++;
            }
            
            
            if (!p_bWithParent) p_oApp.commitTrans();
            
            p_nEditMode = EditMode.UNKNOWN;
            return true;
        } else {    
            if (!p_bWithParent) p_oApp.beginTrans(); 
            lnCtr = 1;
             p_oImages.beforeFirst();
            while (p_oImages.next()){
                String lsInvIDx = p_oImages.getString("sMCInvIDx");
                lsSQL = MiscUtil.rowset2SQL(p_oImages, 
                                            MASTER_TABLE, 
                                            "sBrandNme;sModelNme;sColorNme",
                                            "sMCInvIDx = " + SQLUtil.toSQL(lsInvIDx) +
                                            " AND nEntryNox = " + SQLUtil.toSQL(lnCtr));
                
                if (!lsSQL.isEmpty()){
                    if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, lsInvIDx.substring(0, 4)) <= 0){
                        if (!p_bWithParent) p_oApp.rollbackTrans();
                        p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                        return false;
                    }
                }
                lnCtr++;
            }
            
            
            if (!p_bWithParent) p_oApp.commitTrans();
            
            p_nEditMode = EditMode.UNKNOWN;
            return true;
        }
    }
    
    private boolean isEntryOK() throws SQLException{           
        //validate master        
        //validate detail
        if (getImageCount() == 0){
            p_sMessage = "No employee detected.";
            return false;
        }
        
        p_oImages.beforeFirst();
        while (p_oImages.next()){            
            if (p_oImages.getString("sImageUrl").isEmpty()){
                p_sMessage = p_oImages.getString("xEmployNm") + " has negative incentive total amount.";
                return false;
            }    
        }
        return true;
    }
    
    public String getSQ_Images(){
        String lsSQL = "";
        String lsCondition = "";
        String lsStat = String.valueOf(p_nTranStat);
        
        if (lsStat.length() > 1){
            for (int lnCtr = 0; lnCtr <= lsStat.length()-1; lnCtr++){
                lsSQL += ", " + SQLUtil.toSQL(Character.toString(lsStat.charAt(lnCtr)));
            }
            
            lsCondition = "a.cRecdStat IN (" + lsSQL.substring(2) + ")";
        } else 
            lsCondition = "a.cRecdStat = " + SQLUtil.toSQL(lsStat);
                
        lsSQL = "SELECT " +
                    " a.sMCInvIDx " +
                    ", a.nEntryNox " +
                    ", a.sImageURL " +
                    ", a.cPrimaryx " +
                    ", a.cRecdStat " +
                    ", a.sModified " + 
                    ", a.dModified " +
                    ", e.sBrandNme " +
                    ", c.sModelNme " +
                    ", d.sColorNme " +
                " FROM MC_Model_Images a" +
                "  LEFT JOIN MC_Inventory b" +
                       " ON a.sMCInvIDx = b.sMCInvIDx" +
                    " LEFT JOIN Color e" +
                       " ON b.sColorIDx = e.sColorIDx" +
                    ", MC_Model c" +
                    ", Brand d" +
                " WHERE b.sBrandIDx = d.sBrandIDx" +
                    " AND b.sModelIDx = c.sModelIDx" +
                    " AND b.sBranchCd =  " + SQLUtil.toSQL(p_oApp.getBranchCode()) +
                    lsCondition;
        
        return lsSQL;
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
    
    private void convertJsonArrayToCachedRowSet(JSONArray jsonArrayString) throws SQLException {
        JSONArray jsonArray = jsonArrayString;

        createImages();

        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject)jsonArray.get(i);
            p_oImages.moveToInsertRow();

//            for (String key : jsonObject.keySet()) {
//                Object value = jsonObject.get(key);
//                rowSet.updateObject(key, value);
//            }

            for (Object key : jsonObject.keySet()) {
                Object value = jsonObject.get(key);
                p_oImages.updateObject((String) key, value);
            }

            p_oImages.insertRow();
            p_oImages.moveToCurrentRow();
        }

        p_oImages.acceptChanges();
    }

}
