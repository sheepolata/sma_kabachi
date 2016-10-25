/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package twelveAngryMen;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

/**
 *
 * @author sheep
 */
public class JuryAgent extends Agent{
    //Age de l'agent
    private int age;
    //Opinion de l'agent sur la culpabilité de l'accusé. (-1 < x < 1)
    private float opinion;
    //Valeur représentant la persuasion de l'agent, sa capacité à convaincre les autres jurés. (0 < x < 1)
    private float persuation;
    //Valeur représentant l'obstination de l'agent, sa capacité à ne pas changer d'avis. (0 < x < 1)
    private float persist;
    //Valeur représentant le fait qu'un agent soit intelligent ou non.
    private boolean intelligence;
    //Valeur représentant le fait qu'un agent le temps de réfléchir ou non.
    private boolean thinker;
    //Valeur représentant si l'agent recherche la justice ou non.
    private boolean justice;
    
    
    @Override
    protected void setup(){
        System.out.println("I am " + getAID().getLocalName() + ", and I am part of the jury");
        Object[] args = getArguments();
        
         if (args != null && args.length >= 7){
             age = Integer.parseInt((String)args[0]);
             opinion = Float.parseFloat((String)args[1]);
             persuation = Float.parseFloat((String)args[2]);
             persist = Float.parseFloat((String)args[3]);
             intelligence = Boolean.parseBoolean((String)args[4]);
             thinker = Boolean.parseBoolean((String)args[5]);
             justice = Boolean.parseBoolean((String)args[6]);
             
//             display();

            // Ajout de l'agent et de sa description dans le Directory Facilitator
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            System.out.println("");
            ServiceDescription sd = new ServiceDescription();
            sd.setType("book-selling");
            sd.setName("JADE-book-trading");
            dfd.addServices(sd);
            try {
                    DFService.register(this, dfd);
            }
            catch (FIPAException fe) {
                    fe.printStackTrace();
            }
            
         }else{
             // Make the agent terminate
            System.out.println("Some argument are missing. 7 are expected !");
            doDelete();
         }
    }
    
    @Override
    protected void takeDown(){
        try {
                DFService.deregister(this);
        }
        catch (FIPAException fe) {
                fe.printStackTrace();
        }
    }
    
    public void display(){
        System.out.println("I am " + getAID().getName() + ", " + getAID().getLocalName() + " for short");
        System.out.println(age + " years old");
        System.out.println("Opinion : " + opinion);
        System.out.println("Persuasion : " + persuation);
        System.out.println("Persist : " + persist);
        System.out.println("Intelligence : " + intelligence);
        System.out.println("Thinker : " + thinker);
        System.out.println("Justice : " + justice);
    }
}
