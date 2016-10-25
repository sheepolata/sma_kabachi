/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
 *****************************************************************/

package bookTrader;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class BookBuyerAgent extends Agent {
    // The title of the book to buy
    private String targetBookTitle;

    //--------------------------------------------
    // MODIF : Ajout des deux arguments ci-dessous
    //--------------------------------------------
    // Wallet value
    private int money;
    // Real Price : Price the agent is ready to pay for the book
    private int rp;

    // The list of known seller agents
    private AID[] sellerAgents;

    // Put agent initializations here
    @Override
    protected void setup() {
            // Printout a welcome message
            System.out.println("Hallo! Buyer-agent "+getAID().getName()+" is ready.");

            // Get the money, the title of the book to buy and the real price of the book as a start-up arguments
            Object[] args = getArguments();
            if (args != null && args.length > 2) {
                money           = Integer.parseInt((String)args[0]);
                targetBookTitle = (String) args[1];
                rp              = Integer.parseInt((String)args[2]);
                System.out.println("I have " + money + "$, target book is \""+targetBookTitle + "\", my real price is " + rp);

                // Add a TickerBehaviour that schedules a request to seller agents every minute
                //--------------------------------------------------------
                // MODIF : Toute les 10 secondes et non toutes les minutes
                //--------------------------------------------------------
                addBehaviour(new TickerBehaviour(this, 10000) {
                        @Override
                        protected void onTick() {
                            System.out.println("Trying to buy "+targetBookTitle);
                            // Update the list of seller agents
                            DFAgentDescription template = new DFAgentDescription();
                            ServiceDescription sd = new ServiceDescription();
                            sd.setType("book-selling");
                            template.addServices(sd);
                            try {
                                    DFAgentDescription[] result = DFService.search(myAgent, template); 
                                    System.out.println("Found the following seller agents:");
                                    sellerAgents = new AID[result.length];
                                    for (int i = 0; i < result.length; ++i) {
                                            sellerAgents[i] = result[i].getName();
                                            System.out.println(sellerAgents[i].getName());
                                    }
                            }
                            catch (FIPAException fe) {
                                    fe.printStackTrace();
                            }

                            // Perform the request
                            myAgent.addBehaviour(new RequestPerformer());
                    }
                } );
            }
            else {
                    // Make the agent terminate
                    System.out.println("No target book title specified");
                    doDelete();
            }
    }

    // Put agent clean-up operations here
    @Override
    protected void takeDown() {
            // Printout a dismissal message
            System.out.println("Buyer-agent "+getAID().getName()+" terminating.");
    }

    /**
       Inner class RequestPerformer.
       This is the behaviour used by Book-buyer agents to request seller 
       agents the target book.
     */
    private class RequestPerformer extends Behaviour {
        private AID bestSeller; // The agent who provides the best offer 
        private int bestPrice;  // The best offered price
        private int repliesCnt = 0; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;

        @Override
        public void action() {
            switch (step) {
            case 0:
                // Send the cfp to all sellers
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                for (int i = 0; i < sellerAgents.length; ++i) {
                        cfp.addReceiver(sellerAgents[i]);
                } 
                cfp.setContent(targetBookTitle);
                cfp.setConversationId("book-trade");
                cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                myAgent.send(cfp);
                // Prepare the template to get proposals
                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                                MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                step = 1;
                break;
            case 1:
                // Receive all proposals/refusals from seller agents
                ACLMessage reply = myAgent.receive(mt);
                if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                                // This is an offer 
                                int price = Integer.parseInt(reply.getContent());
                                if (bestSeller == null || price < bestPrice) {
                                        // This is the best offer at present
                                        bestPrice = price;
                                        bestSeller = reply.getSender();
                                }
                        }
                        repliesCnt++;
                        if (repliesCnt >= sellerAgents.length) {
                                // We received all replies
                                step = 2; 
                        }
                }
                else {
                        block();
                }
                break;
            case 2:
                // Modif :  If the agent don't have enought money, he leaves
                if(bestPrice <= money){
                    System.out.println(myAgent.getLocalName() + " say : I don't have enought money, I'm out... =(");
                    step = 4;
                    break;
                }
                //----------------------------------------------------------------------------
                // MODIF : If the agent have enought money and really want the book
                //----------------------------------------------------------------------------
                if(bestPrice <= rp){
                    // Send the purchase order to the seller that provided the best offer
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestSeller);
                    order.setContent(targetBookTitle);
                    order.setConversationId("book-trade");
                    order.setReplyWith("order"+System.currentTimeMillis());
                    myAgent.send(order);
                    // Prepare the template to get the purchase order reply
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                                    MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step = 3;
                }
                // MODIF : Else, the agent will try to negociate with the seller
                else{
                    ///He send the seller a PROPOSE message
                    ACLMessage negociate = new ACLMessage(ACLMessage.PROPOSE);
                    negociate.addReceiver(bestSeller);
                    negociate.setContent(String.valueOf(rp));
                    negociate.setConversationId("book-trade-negiciate");
                    negociate.setReplyWith("negociate"+System.currentTimeMillis());
                    myAgent.send(negociate);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade-negiciate"),
                                    MessageTemplate.MatchInReplyTo(negociate.getReplyWith()));
                    System.out.println(targetBookTitle + " sold by " + bestSeller.getLocalName()+ " is too expensive, let's negociate !");
                    step = 3;
                }
                break;
            case 3:      
                // Receive the purchase order reply
                reply = myAgent.receive(mt);
                if (reply != null) {
                    // Purchase order reply received
                    switch (reply.getPerformative()) {
                        //If the seller send a INFORM, it means that he accepts the deal with the buyers.
                        case ACLMessage.INFORM:
                            // Purchase successful. We can terminate
                            System.out.println(targetBookTitle+" successfully purchased from agent "+reply.getSender().getName());
                            bestPrice = Integer.parseInt(reply.getContent());
                            System.out.println("Price = " + bestPrice);
                            //---------------------------------------------------------
                            // MODIF : When he buy the book, the buyer's money is taken
                            //---------------------------------------------------------
                            money -= bestPrice;
                            myAgent.doDelete();
                            step = 4;
                            break;
                        case ACLMessage.REQUEST:
                            //Is the seller send a REQUEST, it means that he is ready to negociate, 
                            //He will up his tolerance, lower his price ans the buyer have to up his real price
                            System.out.println("Negociation : let's up my real price.");
                            rp += Integer.parseInt(reply.getContent());
                            System.out.println(myAgent.getLocalName() + " say : my real price is now " + rp);
                            //Then, the buyer try to buy the book another time
                            step = 2;
                            break;
                        default:
                            System.out.println("Attempt failed: requested book already sold.");
                            break;
                    }
                }
                else {
                    block();
                }
                break;
            }        
        }

        @Override
        public boolean done() {
                if (step == 2 && bestSeller == null) {
                        System.out.println("Attempt failed: "+targetBookTitle+" not available for sale");
                }
                return ((step == 2 && bestSeller == null) || step == 4);
        }
    }  // End of inner class RequestPerformer
}
