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
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

public class BookSellerAgent extends Agent {
	// The catalogue of books for sale (maps the title of a book to its price)
	private Hashtable catalogue;
        
        // MODIF : Add two new arguments
        //The tolerance of a seller, how much he will be OK tu lower his price
        private int tolerance;
        //The change rate of his tolerance
        private int change;
        
	// The GUI by means of which the user can add books in the catalogue
	private BookSellerGui myGui;
        
        //Current title he want to sell
        private String title;

	// Put agent initializations here
        @Override
	protected void setup() {
            System.out.println("Hallo! Seller-agent "+getAID().getName()+" is ready.");
            Object[] args = getArguments();
            //We get the two first arguments to set the tolerance and the change rate
            if (args != null && args.length > 1) {
		// Create the catalogue
		catalogue = new Hashtable();
                tolerance = Integer.parseInt((String)args[0]);
                change = Integer.parseInt((String)args[1]);
                
                System.out.println("My tolerance is " + tolerance + " and i will change with a rate of " + change);
                
		// Create and show the GUI 
		myGui = new BookSellerGui(this);
		myGui.showGui();

		// Register the book-selling service in the yellow pages
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

		// Add the behaviour serving queries from buyer agents
		addBehaviour(new OfferRequestsServer());

		// Add the behaviour serving purchase orders from buyer agents
		addBehaviour(new PurchaseOrdersServer());
                
                //------------------------------------------------------------------------------
                //MODIF : If the seller has no more book to sell, he is terminated. It is check every minute
                //------------------------------------------------------------------------------
                addBehaviour(new TickerBehaviour(this, 60000) {
                    @Override
                    protected void onTick() {
                        if(catalogue.isEmpty()){
                            myAgent.doDelete();
                        }
                    }
                });
            }
	}

	// Put agent clean-up operations here
        @Override
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Close the GUI
		myGui.dispose();
		// Printout a dismissal message
		System.out.println("Seller-agent "+getAID().getName()+" terminating.");
	}

	/**
     This is invoked by the GUI when the user adds a new book for sale
	 */
	public void updateCatalogue(final String title, final int price) {
		addBehaviour(new OneShotBehaviour() {
                        @Override
			public void action() {
				catalogue.put(title, price);
				System.out.println(title+" inserted into catalogue. Price = "+price);
			}
		} );
	}

	/**
	   Inner class OfferRequestsServer.
	   This is the behaviour used by Book-seller agents to serve incoming requests 
	   for offer from buyer agents.
	   If the requested book is in the local catalogue the seller agent replies 
	   with a PROPOSE message specifying the price. Otherwise a REFUSE message is
	   sent back.
	 */
	private class OfferRequestsServer extends CyclicBehaviour {
                @Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// CFP Message received. Process it
				title = msg.getContent();
				ACLMessage reply = msg.createReply();

				Integer price = (Integer) catalogue.get(title);
				if (price != null) {
					// The requested book is available for sale. Reply with the price
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(price.intValue()));
				}
				else {
					// The requested book is NOT available for sale.
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer

	/**
	   Inner class PurchaseOrdersServer.
	   This is the behaviour used by Book-seller agents to serve incoming 
	   offer acceptances (i.e. purchase orders) from buyer agents.
	   The seller agent removes the purchased book from its catalogue 
	   and replies with an INFORM message to notify the buyer that the
	   purchase has been sucesfully completed.
	 */
	private class PurchaseOrdersServer extends CyclicBehaviour {
                @Override
		public void action() {
                    //We check if the message received is a ACCEPT_PROPOSAL or a new PROPOSE
                    MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                    ACLMessage msg = myAgent.receive(mt);
                    //If it is a ACCEPT_PROPOSAL, the seller sell the book to the buyer (execpt if the book if not here anymore)
                    if (msg != null) {
                        // ACCEPT_PROPOSAL Message received. Process it
                        title = msg.getContent();
                        ACLMessage reply = msg.createReply();

                        Integer price = (Integer) catalogue.remove(title);
                        if (price != null) {
                                reply.setPerformative(ACLMessage.INFORM);
                                reply.setContent(String.valueOf(price));
                                System.out.println(title+" sold to agent "+msg.getSender().getName());
                        }
                        else {
                                // The requested book has been sold to another buyer in the meanwhile .
                                reply.setPerformative(ACLMessage.FAILURE);
                                reply.setContent("not-available");
                        }
                        myAgent.send(reply);
                    }
                    //If it is a new PROPOSE, the buyer is trying to negociate.
                    else {
                        ACLMessage msg2 = myAgent.receive(mt2);
                        if(msg2 != null){
                            ACLMessage reply = msg2.createReply();
                            //If the price of the buyer is OK for the seller according to his tolerance, the transaction is made
                            if(((Integer) catalogue.get(title) - Integer.parseInt(msg2.getContent())) <= tolerance){
                                System.out.println(myAgent.getLocalName() + " say : OK, let's make a deal");
                                updateCatalogue(title, Integer.parseInt(msg2.getContent()));
                                reply.setPerformative(ACLMessage.INFORM);
                                reply.setContent(msg2.getContent());
                            }
                            //Else, the seller accept to negociate, he up his tolerance and ask the buyer to up his real price
                            else{
                                //Send a REQUEST Message to negociate with the buyer
                                reply.setPerformative(ACLMessage.REQUEST);
                                //Up the tolerance by *change*
                                tolerance += change;
                                reply.setContent(String.valueOf(change));
                                System.out.println(myAgent.getLocalName() + " say : Let's up your real price while I up my tolerance (" + ((Integer) catalogue.get(title) - Integer.parseInt(msg2.getContent())) + " < " + tolerance + ")");
                            }
                            myAgent.send(reply);
                        }
                        else{
                            block();
                        }
                    }
		}
	}  // End of inner class OfferRequestsServer
}
