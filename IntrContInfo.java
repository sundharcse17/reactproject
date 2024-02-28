package com.tem.aspire;

import com.temenos.t24.api.hook.system.ServiceLifecycle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangement.CustomerClass;
import com.temenos.t24.api.records.aacustomerarrangement.AaCustomerArrangementRecord;
import com.temenos.t24.api.records.aacustomerarrangement.ArrangementClass;
import com.temenos.t24.api.records.aacustomerarrangement.ProductLineClass;
import com.temenos.t24.api.records.deaddress.DeAddressRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * 
 *
 * @author sundharesh.paramasiv
 *
 */
public class IntrContInfo extends ServiceLifecycle {
    String format="yyMMddHHmm";
    @SuppressWarnings("null")
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        DataAccess dataAccess=new DataAccess(this);
//        List<String> deAddressId=null;
//        List<String>  email=null;
//        List<String> sms=null;
//        List<String> phone=null;
//        email=dataAccess.selectRecords("", "DE.ADDRESS", "","WITH @ID LIKE ...EMAIL.1...");
//        sms=dataAccess.selectRecords("", "DE.ADDRESS", "", "WITH @ID LIKE ...SMS.1...");
//        phone=dataAccess.selectRecords("", "DE.ADDRESS", "", "WITH @ID LIKE ...PHONE.1...");
//        deAddressId.addAll(email);
//        deAddressId.addAll(sms);
//        deAddressId.addAll(phone);
        List<String>  deAddressId=dataAccess.selectRecords("", "DE.ADDRESS", "","WITH @ID LIKE ...EMAIL.1... OR WITH @ID LIKE ...SMS.1...");

        return deAddressId;
    }
    @Override
    public void process(String id, ServiceData serviceData, String controlItem) {
        DataAccess dataAccess = new DataAccess(this);

     
        try
        {
            String[] s=id.split("\\.");
            String[] s1=s[1].split("-");
            String s2=s1[1];
            AaCustomerArrangementRecord al=new AaCustomerArrangementRecord(dataAccess.getRecord("AA.CUSTOMER.ARRANGEMENT", s2));
            List<ProductLineClass> pr=al.getProductLine();
            List<ArrangementClass> arrangementId=null;         
            for(ProductLineClass pc:pr)
            {
                String sd=pc.getProductLine().toString();
                
                if(sd.equals("LENDING"))
                {               
                    arrangementId=pc.getArrangement();
                }
            }
            
            for(int j=0;j<arrangementId.size();j++)
            {
                AaArrangementRecord aaArrangementRecord=new AaArrangementRecord(dataAccess.getRecord("AA.ARRANGEMENT", arrangementId.get(j).getArrangement().toString()));
                CustomerClass customerRecord=aaArrangementRecord.getCustomer(0);
                String customerId=customerRecord.getCustomer().toString();
                if(customerId.equals(s2))
                {
                    DeAddressRecord deAddressRecord=new DeAddressRecord(dataAccess.getRecord("DE.ADDRESS", id));
                    String startDate="";
                    String endDate=null;
                    if(deAddressRecord.getCurrNo().equals("1"))
                    {
                        startDate=deAddressRecord.getDateTime(0).toString();
                        startDate=startDate(startDate);
                    }
                    else
                    {
                        int n1=Integer.parseInt(deAddressRecord.getCurrNo());
                        DeAddressRecord previous=deAddressRecord;
                        endDate=historyEndDate(id,n1,previous);
                    }
                    String currentdate=deAddressRecord.getDateTime(0).toString();                   
                    String arrangeId=arrangementId.get(j).getArrangement().toString();
                    String contactInfoId=id;
                    String rowid="3210";
                    String version="1";
                    String pkey="";
                    String command="U";
                    String primaryFlag="1";
                    String legalFlag="1";
                    String ownerCode=null;
                    String statusCode="V";
                    String sourceCode=null;
                    String updateDateTime= updateTime(currentdate);
                    String debtorPkey=null;
                    Map<String,String> contacts=contactInfo(deAddressRecord,pkey);
                    for (Map.Entry<String,String> entry : contacts.entrySet()) 
                    {
                        pkey=id+entry.getKey()+customerRecord.getCustomer().getValue();
                        String to=updateDateTime+"\t"+rowid+"\t"+version+"\t"+pkey+"\t"+command+"\t"+arrangeId+"\t"+debtorPkey+"\t"+customerRecord.getCustomer().getValue()+"\t"+entry.getKey()+"\t"+entry.getValue()+"\t"+statusCode+"\t"+sourceCode+"\t"
                                +ownerCode+"\t"+startDate+"\t"+endDate+"\t"+contactInfoId+"\t"+"null"+"\t"+primaryFlag+"\t"+legalFlag+"\n";
                        writeFile(to,true);
                    }
                }
            }
        }
        catch(Exception e)
        {
            Logger.getLogger(this.getClass().getSimpleName()).info(e.getMessage());
        }
    }
    private String historyEndDate(String id,int n1,DeAddressRecord previous)
    {
        String endDate=null;
        DataAccess dataAccess=new DataAccess(this);
        for (int i=n1;i>1;i--){
            DeAddressRecord deHistoryRecord=new DeAddressRecord(dataAccess.getRecord("DE.ADDRESS$HIS",  id+";"+(i-1)));

            if(!(previous.getPhone1().getValue().equals(deHistoryRecord.getPhone1().getValue())) || !(previous.getEmail1().getValue().equals(deHistoryRecord.getEmail1().getValue())) || !(previous.getSms1().getValue().equals(deHistoryRecord.getSms1().getValue()))){
                endDate=endDate(previous.getDateTime(0));
                break;
            }
            else
            {
                endDate=null;
            }
        }
        return endDate;
    }

    private Map<String,String> contactInfo(DeAddressRecord deAddressRecord,String pkey)
    {
        Map<String,String> contactInfoDetail=new HashMap<>();
        List<String> rec=new ArrayList<>();
        rec.add(deAddressRecord.getPhone1().toString());
        rec.add(deAddressRecord.getEmail1().toString());
        rec.add(deAddressRecord.getSms1().toString());
        
        
        if(!deAddressRecord.getPhone1().getValue().isEmpty() || (deAddressRecord.getPhone1().getValue()!=""))
        {
            String contant1="P";
            String contentInfo1=deAddressRecord.getPhone1().getValue();
            contactInfoDetail.put(contant1, contentInfo1);
        }
        if(!deAddressRecord.getEmail1().getValue().isEmpty() || (deAddressRecord.getEmail1().getValue()!=""))
        {
            String contant2="E";
            String contentInfo2=deAddressRecord.getEmail1().getValue();
            contactInfoDetail.put(contant2, contentInfo2);
        }
        if(!deAddressRecord.getSms1().getValue().isEmpty() || (deAddressRecord.getSms1().getValue()!=""))
        {
            String contant3="S";
            String contentInfo3=deAddressRecord.getSms1().getValue();
            contactInfoDetail.put(contant3, contentInfo3);
        }
        else
        {
            for(int i=0;i<rec.size();i++)
            {
                if(rec.get(i)=="" || rec.get(i).isEmpty())
                {
                    String contant4="O";
                    String contentInfo4=null;
                    contactInfoDetail.put(contant4, contentInfo4);
                }
            }
          
        }
        return contactInfoDetail;
    }

    private String endDate(String endDate)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        SimpleDateFormat dateString = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = dateFormat.parse(endDate);
            return dateString.format(date);

        } catch (ParseException e) {
            Logger.getLogger(this.getClass().getSimpleName()).info(e.getMessage());
        }
        return null;
    }
    private String startDate(String startDate)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        SimpleDateFormat dateString = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = dateFormat.parse(startDate);
            startDate = dateString.format(date);
            return startDate;

        } catch (ParseException e) {

            Logger.getLogger(this.getClass().getSimpleName()).info(e.getMessage());
        }
        return null;
    }
    private String updateTime(String currentdate)
    {
        String dateAsString = null;
        String dateAsString1 = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        SimpleDateFormat dateString = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat dateString1 = new SimpleDateFormat("hhMMSS");
        try {
            Date date = dateFormat.parse(currentdate);
            dateAsString = dateString.format(date);
            dateAsString1 = dateString1.format(date);

        } catch (ParseException e) {
            Logger.getLogger(this.getClass().getSimpleName()).info(e.getMessage());
        }
        return dateAsString+"T"+dateAsString1;
    }
    private void writeFile(String finalArrayPrint, boolean setVal) {
        String file="C:\\FILE01\\Catalyst";
        File f=new File(file);
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(f, setVal))) {
            boolean exist=!f.createNewFile();

            bufferedWriter.newLine();
            if(exist)
            {
                exist=true;
            }
            bufferedWriter.write((new StringBuilder(finalArrayPrint).toString()));
        } catch (IOException e) {
            Logger.getLogger(this.getClass().getSimpleName()).info(e.getMessage());
        }     
    }
	
	private void maingit()
	{}
	
	private void featurebranch()
	{}
}
