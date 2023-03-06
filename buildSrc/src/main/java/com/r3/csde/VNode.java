package com.r3.csde;

import java.util.List;

public class VNode{
    private String x500Name;
    private List<String> cpis;

    private String serviceX500Name;

    public VNode() {
    }

    public String getX500Name(){ return x500Name; }
    public void setX500Name(String _x500Name) { x500Name = _x500Name;}

    public List<String> getCpis() { return cpis; }
    public void setCpis(List<String> _cpis) { cpis = _cpis; }

    public String getServiceX500Name() { return serviceX500Name; }
    public void setServiceX500Name(String _name) {serviceX500Name = _name; }

}
