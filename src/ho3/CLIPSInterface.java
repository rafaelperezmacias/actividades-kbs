package ho3;

import java.util.List;
import net.sf.clipsrules.jni.CLIPSException;
import net.sf.clipsrules.jni.CLIPSLoadException;
import net.sf.clipsrules.jni.Environment;
import net.sf.clipsrules.jni.FactAddressValue;
import net.sf.clipsrules.jni.FactInstance;
import net.sf.clipsrules.jni.PrimitiveValue;
import net.sf.clipsrules.jni.SlotValue;

public class CLIPSInterface {

    private Environment clips;
    
    public CLIPSInterface()
    {
        clips = new Environment();
    }
    
    public void addConstructor(String constructor) {
        try {
            clips.build(constructor);
        } catch (CLIPSException ex) {
            ex.printStackTrace();
        }
    }
    
    public void addTemplate(String template) {
        addConstructor(template);
    }
    
    public void addFactsWithConstructor(String facts) {
        addConstructor(facts);
    }
    
    public FactAddressValue assertFactWithTemplate(String fact) {
        try {  
            return clips.assertString(fact);
        } catch (CLIPSException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    public PrimitiveValue addFact(String fact) {
        try {
            return clips.eval(fact);
        } catch (CLIPSException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    public List<FactInstance> showAllFacts() {
        List<FactInstance> facts = clips.getFactList();
        for ( FactInstance fact : facts ) {
            System.out.print(fact.getName() + " " + fact.getRelationName() + " => (");
            for ( SlotValue value : fact.getSlotValues() ) {
                System.out.print("(" + value.getSlotName() + " " + value.getSlotValue() +") ");
            }
            System.out.println(")");
        }
        return facts;
    }
    
    public List<FactAddressValue> showAllFactsOfTheTemplate(boolean print, String deftemplateName, String... slots) {
        List<FactAddressValue> facts = null;
        try {
            facts = clips.findAllFacts(deftemplateName);
            for ( FactAddressValue value : facts ) {
                String out = "(" + deftemplateName + " (";
                for ( String slot : slots ) {
                        out += " (" + value.getSlotValue(slot) + ") ";
                }
                out += ") )";
                if ( print ) {
                    System.out.println(out);
                }
            }
        } catch (CLIPSException ex) {
            ex.printStackTrace();
        }
        return facts;
    }
    
    public List<FactAddressValue> showAllFactsOfTheTemplateWithRule(boolean print, String deftemplateName, String variable, String condition, String... slots) {
        List<FactAddressValue> facts = null;
        try {
            facts = clips.findAllFacts(variable, deftemplateName, condition);
            for ( FactAddressValue value : facts ) {
                String out = "(" + deftemplateName + " (";
                for ( String slot : slots ) {
                        out += " (" + value.getSlotValue(slot) + ") ";
                }
                out += ") )";
                if ( print ) {
                    System.out.println(out);
                }
            }
        } catch (CLIPSException ex) {
            ex.printStackTrace();
        }
        return facts;
    }
    
    public void addRule(String rule) {
        addConstructor(rule);
    }
    
    public void showRules() {
        try {
            System.out.println(clips.eval("(rules)"));
        } catch (CLIPSException ex) {
            ex.printStackTrace();
        }
    }
    
    public long run() {
        try {
            return clips.run();
        } catch (CLIPSException ex) {
            ex.printStackTrace();
        }
        return -1;
    }
    
    public long run(long runLimit) {
        try {
            return clips.run(runLimit);
        } catch (CLIPSException ex) {
            ex.printStackTrace();
        }
        return -1;
    }
    
    public void reset() {
        try {
            clips.reset();
        } catch (CLIPSException ex) {
            ex.printStackTrace();
        }
    }
    
    public void clear() {
        try {
            clips.clear();
        } catch (CLIPSException ex) {
            ex.printStackTrace();
        }
    }
    
    public void loadFile(String path) {
        try {
            clips.load(path);
        } catch (CLIPSLoadException ex) {
            ex.printStackTrace();
        }
    }
     
}
