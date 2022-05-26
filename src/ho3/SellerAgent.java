package ho3;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.FailureException;

import java.util.*;
import net.sf.clipsrules.jni.FactAddressValue;

public class SellerAgent extends Agent {

	private CLIPSInterface clips;

    protected void setup() {
		// No kbs
		Object[] args = getArguments();
		if ( args == null || args.length == 0 ) {
            System.out.println("Agent " + getLocalName() + ": no kb specified");
            doDelete();
            return;
        }

		// Setup fot the kbs
		ArrayList<String> files = new ArrayList<>();
		files.add("src\\ho3\\kbs\\templates.clp");
		if ( ((String) args[0]).equals("1") ) {
			files.add("src\\ho3\\kbs\\1\\facts.clp");
			files.add("src\\ho3\\kbs\\1\\rules.clp");
		} else if ( ((String) args[0]).equals("2") ) {
			files.add("src\\ho3\\kbs\\2\\facts.clp");
			files.add("src\\ho3\\kbs\\2\\rules.clp");
		} else if ( ((String) args[0]).equals("3") ) {
			files.add("src\\ho3\\kbs\\3\\facts.clp");
			files.add("src\\ho3\\kbs\\3\\rules.clp");
		} else {
			System.out.println("Agent " + getLocalName() + ": kb invalid");
			doDelete();
			return;
		}

		// Kbs
		System.out.println("Seller-agent " + getLocalName() + ": updating kb");
		clips = new CLIPSInterface();
		updateKB(files);
		clips.showAllFacts();

		// Ready
        System.out.println("Seller-agent " + getLocalName() + " ready, waiting for CFP...");

		// Contract-Net-Protocol
        MessageTemplate template = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
				MessageTemplate.MatchPerformative(ACLMessage.CFP) );

		addBehaviour(new ContractNetResponder(this, template) {
			
			@Override
			protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
				System.out.println("Agent " + getLocalName() + ": CFP received from " + cfp.getSender().getLocalName());
				
				String content = cfp.getContent();
				String[] facts = content.split("\n");
				String contentResponse = "";
				Set<String> ids = new HashSet<>();

				for ( String fact : facts ) {
					clips.assertFactWithTemplate(fact);
				}
				long rules = clips.run();

				if ( rules > 0 ) {
					System.out.println("Agent " + getLocalName() + ": offers =>");
					String customerId = getAttributeFromOrder(cfp.getContent(), "customer-id");
					List<FactAddressValue> offers = clips.showAllFactsOfTheTemplateWithRule(true, "result", "?r", "(eq ?r:customer-id " + customerId + ")", "customer-id", "offer", "description", "price", "product-id", "order-id", "amount");
					for ( FactAddressValue offer : offers ) {
						contentResponse += "(result (customer-id " + offer.getSlotValue("customer-id") + ") ";
						contentResponse += "(offer " + offer.getSlotValue("offer") + ") ";
						contentResponse += "(description " + offer.getSlotValue("description") + ") ";
						contentResponse += "(product-id " + offer.getSlotValue("product-id") + ") ";
						contentResponse += "(price " + offer.getSlotValue("price") + ") ";
						contentResponse += "(order-id " + offer.getSlotValue("order-id") + ") ";
						contentResponse += "(amount " + offer.getSlotValue("amount") + "))\n";
						if ( !offer.getSlotValue("offer").toString().contains("FyM" ) ) {
							ids.add(offer.getSlotValue("product-id").toString());
						}
					}
				}

				nextFact:
				for ( String fact : facts ) {
					if ( !fact.startsWith("(line-item") ) {
						continue;
					}
					String productId = getAttributeFromResponse(fact, "product-id");
					for ( String id : ids ) {
						if ( productId.equals(id) ) {
							continue nextFact; 
						}
					}

					List<FactAddressValue> products = clips.showAllFactsOfTheTemplateWithRule(false, "product", "?p", "(eq ?p:id " + productId + ")", "id", "amount", "price");
					if ( products.size() == 0 && checkAction() ) {
						contentResponse += "(result (customer-id " + getAttributeFromOrder(cfp.getContent(), "customer-id") + ") ";
						contentResponse += "(offer N/A) ";
						contentResponse += "(description \"Producto simulado\") ";
						contentResponse += "(product-id " + getAttributeFromResponse(fact, "product-id") + ") ";
						contentResponse += "(price " + (int) ((Math.random() * 40000) + 10000) + ") ";
						contentResponse += "(order-id " + getAttributeFromOrder(cfp.getContent(), "order-number") + ") ";
						contentResponse += "(amount " + (int) ((Math.random() * 6) + 2) + "))\n";
					} else if ( products.size() > 0 ) {
						double amount = Double.parseDouble(products.get(0).getSlotValue("amount").toString());
						if ( amount > 0 ) {
							contentResponse += "(result (customer-id " + getAttributeFromOrder(cfp.getContent(), "customer-id") + ") ";
							contentResponse += "(offer N/A) ";
							contentResponse += "(description \"Ninguna oferta disparada\") ";
							contentResponse += "(product-id " + getAttributeFromResponse(fact, "id") + ") ";
							contentResponse += "(price " + products.get(0).getSlotValue("price").toString() + ") ";
							contentResponse += "(order-id " + getAttributeFromOrder(cfp.getContent(), "order-number") + ") ";
							contentResponse += "(amount " + products.get(0).getSlotValue("amount").toString() + "))\n";
						}
					}
				}

				if ( contentResponse.equals("") ) {
					System.out.println("Agent " + getLocalName() + ": Refuse (products are not available)");
					throw new RefuseException("Products are not available");
				} else {
					System.out.println("Agent " + getLocalName() + ": Proposing to " + cfp.getSender().getLocalName());
					ACLMessage propose = cfp.createReply();
					propose.setPerformative(ACLMessage.PROPOSE);
					propose.setContent(contentResponse);
					return propose;
				}
			}

			@Override
			protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
				System.out.println("Agent " + getLocalName() + ": Proposal rejected from " + reject.getSender().getLocalName());
			}

			@Override
			protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
				System.out.println("Agent " + getLocalName() + ": Proposal accepted from " + accept.getSender().getLocalName());
				System.out.println(accept.getContent());
				String facts[] = accept.getContent().split("\n");
				int i = 0;
				for ( String fact : facts ) {
					String productId = getAttributeFromResponse(fact, "product-id");
					int purchase = Integer.parseInt(getAttributeFromResponse(fact, "purchase"));
					String orderId = getAttributeFromOrder(cfp.getContent(), "order-number");
					String customerId = getAttributeFromResponse(fact, "customer-id");
					List<FactAddressValue> offers = clips.showAllFactsOfTheTemplateWithRule(false, "product", "?p", "(eq ?p:id " + productId + ")", "id", "amount");
					i++;
					for ( FactAddressValue offer : offers ) {
						int amount = Integer.parseInt(offer.getSlotValue("amount").getValue().toString());
						if ( purchase > amount ) {
							throw new FailureException("Product " + productId + " is out of stock (only " + amount + " in stock)");
						}
						String updateRule = "(defrule myrule-" + orderId + productId + " ";
						updateRule += "?p <- (product (id " + productId + ")) ";
						updateRule += "?o <- (order (order-number " + orderId + ") (customer-id " + customerId + ") (finish no)) ";
						updateRule += "=> ";
						if ( i == facts.length ) {
							updateRule += "(modify ?p (amount " + (amount - purchase) + "))";
							updateRule += "(modify ?o (finish yes)))";
						} else {
							updateRule += "(modify ?p (amount " + (amount - purchase) + ")))";
						}
						clips.addRule(updateRule);
						long rules = clips.run();
						if ( rules == 0 ) {
							throw new FailureException("An error occurred with the purchase, try again later.");
						}
					}
				}
				System.out.println("Agent " + getLocalName() + ": purchase order placed");
				System.out.println("Agent " + getLocalName() + ": KB update");
				clips.showAllFactsOfTheTemplate(true, "product", "id", "category", "marca", "model", "price", "amount");
				ACLMessage inform = accept.createReply();
				inform.setPerformative(ACLMessage.INFORM);
				inform.setContent(accept.getContent());
				return inform;
            }

		} );

    }

	
    private String getAttributeFromResponse(String Response, String atribute) {
        return Response.substring(Response.indexOf(atribute) + atribute.length(), Response.indexOf(")", Response.indexOf(atribute) + atribute.length())).trim();
	}

	private String getAttributeFromOrder(String order, String atribute) {
		String orders[] = order.split("\n");
		String subOrder = orders[0];
        return subOrder.substring(subOrder.indexOf(atribute) + atribute.length(), subOrder.indexOf(")", subOrder.indexOf(atribute) + atribute.length())).trim();
	}

	private boolean checkAction() {
		// Simulate a check by generating a random number
		return (Math.random() > 0.2);
	}
	
	private boolean performAction() {
		// Simulate action execution by generating a random number
		return (Math.random() > 0.2);
	}

	private void updateKB(ArrayList<String> pathsOfFilesToLoad) {
		clips.clear();
		for ( String path : pathsOfFilesToLoad ) {
			clips.loadFile(path);
		}
		clips.reset();
	}

}