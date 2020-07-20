package uk.ac.rdg.resc.edal.ncwms.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class NcwmsIndexDatabase implements IndexDatabaseInfo {
    @XmlElement(name = "name")
    private String name = "";

    @XmlElement(name = "url")
    private String url = "";

    @XmlElement(name = "datasetsQuery")
    private String datasetsQuery = "";

    @XmlElement(name = "variablesQuery")
    private String variablesQuery = "";

    NcwmsIndexDatabase() {
    }

    public NcwmsIndexDatabase(
            String name, String url, String datasetsQuery, String variablesQuery
    ) {
        super();
        this.name = name;
        this.url = url;
        this.datasetsQuery = datasetsQuery;
        this.variablesQuery = variablesQuery;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUrl() { return url; }

    @Override
    public String getDatasetsQuery() { return datasetsQuery; }

    @Override
    public String getVariablesQuery() { return variablesQuery; }

    @Override
    public String toString() {
        String s = "";
        s += "Name: " + name;
        return s;
    }
}
