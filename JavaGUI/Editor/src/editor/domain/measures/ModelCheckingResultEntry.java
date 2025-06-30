/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package editor.domain.measures;

import editor.domain.NetPage;
import editor.domain.grammar.EvaluatedBinding;
import editor.domain.io.XmlExchangeDirection;
import editor.domain.io.XmlExchangeException;
import java.util.Scanner;
import org.w3c.dom.Element;

/** The boolean result of CTL model checking.
 *
 * @author elvio
 */
public class ModelCheckingResultEntry extends ResultEntry {
    
    // The computed result.
    // Can be either a boolean for standard CTL/LTL results, or an Integer
    // for bound CTL queries (like:  bound(Place1,Place2) ).
    private Object response;
    private String additionals = null;
    
    public ModelCheckingResultEntry() { }

    public ModelCheckingResultEntry(String entryName, EvaluatedBinding assign) {
        super(entryName, assign);
    }
    
    public void setSatValue(boolean value) {
        this.response = value;
        setComputedOk();
    }
    
    public void setIntValue(int value) {
        this.response = value;
        setComputedOk();
    }

    @Override
    protected String resultToString() {
        String out = response.toString();
        if (additionals!=null)
            out = out + " ("+additionals+")";
        return out;
    }

    @Override
    protected void parseResult(Scanner scanner, NetPage targetNet) {
        String resp = scanner.next();
        
        boolean isTrue = resp.equalsIgnoreCase("true");
        boolean isFalse = resp.equalsIgnoreCase("false");
        if (isTrue || isFalse) {
            setSatValue(isTrue);
            
            if (scanner.hasNext()) {
                additionals = scanner.next();
            }
        }
//        if (resp.equalsIgnoreCase("true"))
//            setSatValue(true);
//        else if (resp.equalsIgnoreCase("false"))
//            setSatValue(false);
        else {
            // Must be an integer value.
            int result = Integer.parseInt(resp);
            setIntValue(result);
        }
    }
    
    @Override
    protected boolean isMultiLine() {
        return false;
    }

    @Override
    public void exchangeXML(Element el, XmlExchangeDirection exDir) throws XmlExchangeException {
        super.exchangeXML(el, exDir); 
        
        if (isComputedOk()) {
            if (exDir.FieldsToXml()) {
                String out = response.toString() + (additionals==null ? "" : " "+additionals);
                el.setAttribute("value", out);
            }
            else {
                Scanner sc = new Scanner(el.getAttribute("value"));
                parseResult(sc, null);
            }
        }
    }
}
