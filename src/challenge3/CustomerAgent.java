package challenge3;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import jade.domain.FIPANames;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.swing.JOptionPane;

public class CustomerAgent extends Agent {

    private int nResponders;

    private static int COUNT_ID = 0;

    private int amount;
    private ArrayList<String> ids;

    private int nResponses;
    private int totalRespones;

    private AID[] sellerAgents;

    protected void setup() {
        Object[] args = getArguments();

        System.out.println("Agent " + getLocalName() + ": is ready");

        // Orders
        String[] orders = new String[]{ "1","2","3","4","5" };
        String orderSelected = (String) JOptionPane.showInputDialog(null, "Select the order", "Customer Agent", JOptionPane.INFORMATION_MESSAGE, null, orders, orders[0]);
        if ( orderSelected == null ) {
            System.out.println("Agent " + getLocalName() + ": no order selected");
            doDelete();
            return;
        }

        // Order
        String order = "";
        long idOrder = System.currentTimeMillis();
        int idCustomer = ++COUNT_ID;
        amount = (int) ((Math.random() * 6) + 2);
        ids = new ArrayList<>();
        switch ( orderSelected ) {
            case "1": {
                order += "(order (order-number " + idOrder + ") (customer-id " + idCustomer + ") (pay-type card))\n";
                order += "(card (order-number " + idOrder + ") (type credit) (bank Banamex))\n";
                order += "(line-item (order-number " + idOrder + ") (product-id 2) (quantity " + amount + "))";
                ids.add("2");
            } break;
            case "2": {
                order += "(order (order-number " + idOrder + ") (customer-id " + idCustomer + ") (pay-type card))\n";
                order += "(card (order-number " + idOrder + ") (type credit) (bank \"Liverpool Visa\"))\n";
                order += "(line-item (order-number " + idOrder + ") (product-id 4) (quantity " + amount + "))";
                ids.add("4");
            } break;
            case "3": {
                order += "(order (order-number " + idOrder + ") (customer-id " + idCustomer + ") (pay-type cash))\n";
                order += "(line-item (order-number " + idOrder + ") (product-id 1) (quantity " + amount + "))\n";
                order += "(line-item (order-number " + idOrder + ") (product-id 3) (quantity " + amount + "))";
                ids.add("1");
                ids.add("3");
            } break;
            case "4": {
                order += "(order (order-number " + idOrder + ") (customer-id " + idCustomer + ") (pay-type cash))\n";
                order += "(line-item (order-number " + idOrder + ") (product-id 22) (quantity " + amount + "))\n";
                ids.add("22");
            } break;
            case "5": {
                order += "(order (order-number " + idOrder + ") (customer-id " + idCustomer + ") (pay-type cash))\n";
                order += "(line-item (order-number " + idOrder + ") (product-id 2) (quantity " + amount + "))\n";
                order += "(line-item (order-number " + idOrder + ") (product-id 22) (quantity " + amount + "))\n";
                ids.add("2");
                ids.add("22");
            } break;
        }

        System.out.println("Agent " + getLocalName() + ": order => " + order);
        
        // Search for service  
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.addLanguages("clips");
        sd.setType("product-selling");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if ( result.length == 0 ) {
                System.out.println("Agent " + getLocalName() + ": no sellers found");
                doDelete();
                return;
            } else {
                System.out.println("Agent " + getLocalName() + ": Found the following seller agents:");
                sellerAgents = new AID[result.length];
                for (int i = 0; i < result.length; ++i) {
                    sellerAgents[i] = result[i].getName();
                    System.out.println(sellerAgents[i].getName());
                }
            }
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // CFP
        ACLMessage msg = new ACLMessage(ACLMessage.CFP);
        for ( int i = 0; i < sellerAgents.length; i++ ) {
            msg.addReceiver(sellerAgents[i]);
        }
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
        msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
        msg.setConversationId("market-shop");
        msg.setLanguage("clips");
        msg.setContent(order);
        
        addBehaviour(new ContractNetInitiator(this, msg) {
            
            @Override
            protected void handleFailure(ACLMessage failure) {
                if ( failure.getSender().equals(myAgent.getAMS()) ) {
                    // FAILURE notification from the JADE runtime: the receiver
                    // does not exist
                    System.out.println("Agent " + getLocalName() + ": responder does not exist");
                }
                else {
                    System.out.println("Agent " + getLocalName() + ": " + failure.getSender().getLocalName() + " failed");
                    System.out.println("Agent " + getLocalName() + ": " + failure.getContent());
                }
                // Immediate failure --> we will not receive a response from this agent
                nResponders--;
            }

            @Override
            protected void handlePropose(ACLMessage propose, Vector v) {
                System.out.println("Agent " + getLocalName() + ": agent " + propose.getSender().getLocalName()+ " proposed: " + propose.getContent());
            }
            
            @Override
            protected void handleRefuse(ACLMessage refuse) {
                System.out.println("Agent " + getLocalName() + ": agent " + refuse.getSender().getLocalName() + " refused: " + refuse.getContent() );
            }
            
            @Override
            protected void handleAllResponses(Vector responses, Vector acceptances) {
                if (responses.size() < nResponders) {
                    // Some responder didn't reply within the specified timeout
                    System.out.println("Timeout expired: missing " + (nResponders - responses.size()) + " responses");
                }
                // Reject all proposals
                Enumeration e = responses.elements();
                ArrayList<Response> responsesMSG = new ArrayList<>();
                while ( e.hasMoreElements() ) {
                    ACLMessage msg = (ACLMessage) e.nextElement();
                    if ( msg.getPerformative() == ACLMessage.PROPOSE ) {
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        acceptances.addElement(reply);
                        System.out.println("Agent " + getLocalName() + ": offers of the " + msg.getSender().getLocalName() + " -> " + msg.getContent());
                        Response response = new Response();
                        response.nameSeller = msg.getSender();
                        response.message = reply;
                        responsesMSG.add(response);
                    }
                }
                // Evaluate proposals for product
                ArrayList<Accept> accepts = new ArrayList<>();  
                for ( String id : ids ) {
                    Accept accept = new Accept();
                    accept.idProduct = id;
                    ArrayList<Product> products = new ArrayList<>();

                    e = responses.elements();
                    HashMap<String, ArrayList<Double>> productsOffer = new HashMap<>();
                    while ( e.hasMoreElements() ) {
                        ACLMessage msg = (ACLMessage) e.nextElement();
                        if ( msg.getPerformative() == ACLMessage.PROPOSE ) {
                            String[] offers = msg.getContent().split("\n");
                            Product lastProduct = null;
                            for ( String offer : offers ) {
                                String idProduct = getAttributeFromOffer(offer,"product-id");
                                if ( !idProduct.equals(id) && !getAttributeFromOffer(offer,"offer").contains("vx") ) {
                                    continue;
                                }

                                Product product = searchProduct(products, id, msg.getSender().getLocalName());
                                if ( product == null ) {
                                    product = new Product();
                                    product.offers = new ArrayList<>();
                                    product.idSeller = msg.getSender();
                                    product.id = id;
                                    products.add(product);
                                }
                                if ( !getAttributeFromOffer(offer,"offer").contains("FyM") ) {
                                    int amoutOffer = Integer.parseInt(getAttributeFromOffer(offer, "amount"));
                                    product.amount = amoutOffer;
                                }
                                if ( lastProduct != null &&  getAttributeFromOffer(offer,"offer").contains("vx") ) {
                                    if ( lastProduct.amount > product.amount ) {
                                        lastProduct.amount = product.amount;
                                    } else if ( lastProduct.amount < product.amount ) {
                                        product.amount = lastProduct.amount;
                                    }
                                }
                                product.price += getValue(offer); 
                                product.offers.add(offer);
                                lastProduct = product;
                            }
                        }
                    }

                    for ( Product product : products ) {
                        System.out.println(product.idSeller.getLocalName() + " -> " + product.price + " for product-id " + product.id);
                    }

                    ArrayList<Product> purchaseProducts = new ArrayList<>();
                    int purchaseAmount = 0;
                    while ( purchaseAmount < amount ) {
                        if ( products.size() == 0 ) {
                            break;
                        }
                        double bestPrice = Double.MAX_VALUE;
                        Product bestProduct = null;
                        for ( Product product : products ) {
                            if ( product.price < bestPrice ) {
                                bestProduct = product;
                                bestPrice = product.price;
                            }
                        }
                        if ( bestProduct != null ) {
                            if ( bestProduct.amount + purchaseAmount > amount ) {
                                bestProduct.purchase = amount - purchaseAmount;
                                purchaseAmount += amount - purchaseAmount;
                            } else {
                                bestProduct.purchase = bestProduct.amount;
                                purchaseAmount += bestProduct.amount;
                            }
                            purchaseProducts.add(bestProduct);
                            products.remove(bestProduct);
                        }
                    }

                    if ( purchaseAmount == amount ) {
                        accept.products = purchaseProducts;
                        accepts.add(accept);
                    } else {
                        System.out.println("Agent " + getLocalName() + ": item " + id + " could not be purchased, not enough parts available, total " + purchaseAmount);
                    }

                }

                // Acept N proposals 
                ArrayList<Seller> sellers = new ArrayList<>();
                for ( Accept accept : accepts ) {
                    System.out.println("\nAccept offers for product " + accept.idProduct);
                    for ( Product product : accept.products ) {
                        System.out.println(product.idSeller.getLocalName() + " -> " );
                        for ( String offer : product.offers ) {
                            System.out.println(offer);
                        }
                        System.out.println("Price " + product.price + ", purchase " + product.purchase);
                        Seller seller = searchSeller(sellers, product.idSeller.getLocalName());
                        if ( seller == null ) {
                            seller = new Seller();
                            seller.name = product.idSeller;
                            seller.products = new ArrayList<>();
                            sellers.add(seller);
                        } 
                        seller.products.add(product);
                    }
                }

                totalRespones = sellers.size();
                for ( Seller seller : sellers ) {
                    String contentResponse = "";
                    for ( Product product : seller.products ) {
                        contentResponse += "((product-id " + product.id + ") ";
                        contentResponse += "(customer-id " + idCustomer + ") ";
                        contentResponse += "(purchase " + product.purchase + "))\n";
                        
                    }
                    for ( Response response : responsesMSG ) {
                        if ( response.nameSeller.getLocalName().equals(seller.name.getLocalName()) ) {
                            response.message.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            response.message.setContent(contentResponse);
                        }
                    }
                }				
            }
            
            @Override
            protected void handleInform(ACLMessage inform) {
                nResponses++;
                System.out.println("Agent " + getLocalName() + ": has completed his purchase order, server by " + inform.getSender().getLocalName() + "\n" + inform.getContent());
                if ( nResponses == totalRespones ) {
                    doDelete();
                }
            }

        } );
    }  

    private Product searchProduct(ArrayList<Product> products, String id, String name) {
        for ( Product product : products ) {
            if ( product.id == id && product.idSeller.getLocalName().equals(name) ) {
                return product;
            }
        }
        return null;
    }

    private Seller searchSeller(ArrayList<Seller> sellers, String name) {
        for ( Seller seller : sellers ) {
            if ( seller.name.getLocalName().equals(name) ) {
                return seller;
            }
        }
        return null;
    }

    private String getAttributeFromOffer(String offer, String atribute) {
        return offer.substring(offer.indexOf(atribute) + atribute.length(), offer.indexOf(")", offer.indexOf(atribute) + atribute.length())).trim();
	}

    private double getValue(String offerChain) {
        String offer = offerChain.substring(offerChain.indexOf("offer") + "offer".length(), offerChain.indexOf(")", offerChain.indexOf("offer"))).trim();
        String description = offerChain.substring(offerChain.indexOf("description") + "description".length(), offerChain.indexOf(")", offerChain.indexOf("description"))).trim();
        String productId = offerChain.substring(offerChain.indexOf("product-id") + "product-id".length(), offerChain.indexOf(")", offerChain.indexOf("product-id"))).trim();
        String price = offerChain.substring(offerChain.indexOf("price") + "price".length(), offerChain.indexOf(")", offerChain.indexOf("price"))).trim();
        double doublePrice = Double.parseDouble(price);
        if ( offer.contains("FyM") ) {
            int desc = Integer.parseInt(offer.substring(offer.indexOf("FyM") + "Fym".length(), offer.length() - 1));
            double mica = Math.random() * 1000;
            double funda = Math.random() * 1000;
            doublePrice = (mica - ( mica * ((double) desc / 100) )) + (funda - ( funda * ((double) desc / 100) ));
        } else if ( offer.contains("M") ) {
            int months = Integer.parseInt(offer.substring(0, offer.indexOf("M")));
            doublePrice /= months;
        } else if ( offer.contains("vx") ) {
            int coupons = Integer.parseInt(offer.substring(0,offer.indexOf("vx")).trim());
            int amount = Integer.parseInt(offer.substring(offer.indexOf("vx") + "vx".length(), offer.indexOf("c")));
            doublePrice -= (coupons * ((int) (doublePrice / amount)));
        }
        return doublePrice;
    }

    private class Response {

        public AID nameSeller;
        public ACLMessage message;

    }

    private class Seller {

        public AID name;
        private ArrayList<Product> products;

    }

    private class Accept {
        
        public String idProduct;
        public ArrayList<Product> products;
        
    }
    
    private class Product {
        
        public String id;
        public AID idSeller;
        public double price;
        public int amount;
        public ArrayList<String> offers;
        public int purchase; 

    }

}