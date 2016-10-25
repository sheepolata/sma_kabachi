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
    //Opinion de l'agent sur la culpabilité de l'accusé (-1 < x < 1)
    private float opinion;
    //Valeur représentant la persuasion de l'agent, sa capacité à convaincre les autres jurés. (0 < x < 1)
    private float persuation;
    //Valeur représentant l'obstination de l'agent, sa capacité à ne pas changer d'avis. (0 < x < 1)
    private float persist;
}
