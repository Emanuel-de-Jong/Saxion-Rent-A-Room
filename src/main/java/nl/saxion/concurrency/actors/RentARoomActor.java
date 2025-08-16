package nl.saxion.concurrency.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import nl.saxion.concurrency.messages.RentARoomMessage;

import java.util.UUID;

public class RentARoomActor extends AbstractBehavior<RentARoomMessage> {

    private final ActorRef<RentARoomMessage> agentActorGroup;


    public RentARoomActor(ActorContext<RentARoomMessage> context) {
        super(context);

        GroupRouter<RentARoomMessage> group = Routers.group(AgentActor.AGENT_ACTOR_SERVICE_KEY).withRoundRobinRouting();
        agentActorGroup = context.spawn(group, "AgentActorGroup");

        // Spawn the first AgentActor
        context.spawn(AgentActor.create(), "AgentActor-" + UUID.randomUUID());
    }

    public static Behavior<RentARoomMessage> create() {
        return Behaviors.setup(RentARoomActor::new);
    }


    @Override
    public Receive<RentARoomMessage> createReceive() {
        return newReceiveBuilder()
                .onMessage(RentARoomMessage.AddAgent.class, this::addAgent)
                .onMessage(RentARoomMessage.class, this::routeToAgentActor)
                .build();
    }


    private Behavior<RentARoomMessage> addAgent(RentARoomMessage.AddAgent message) {
        getContext().spawn(AgentActor.create(), "AgentActor-" + UUID.randomUUID());
        message.sender.tell(new RentARoomMessage.Response("A new agent has been added."));
        return Behaviors.same();
    }

    /**
     * Sends the given message to one of the AgentActors in the agentActorGroup.
     */
    private Behavior<RentARoomMessage> routeToAgentActor(RentARoomMessage message) {
        agentActorGroup.tell(message);
        return Behaviors.same();
    }

}
