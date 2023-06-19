package nl.rug.ds.bpm.specification.jaxb;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * Created by p256867 on 6-4-2017.
 */

@XmlRootElement
public class Formula {
    private String formula, language;

    public Formula() {}
    public Formula(String formula, String language) {
        setFormula(formula);
        setLanguage(language);
    }

    @XmlAttribute
    public void setLanguage(String language) { this.language = language; }
    public String getLanguage() { return language; }

    @XmlValue
    public void setFormula(String formula) { this.formula = formula; }
    public String getFormula() { return formula; }
}
