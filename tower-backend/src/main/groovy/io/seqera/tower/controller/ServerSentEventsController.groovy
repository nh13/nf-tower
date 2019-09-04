package io.seqera.tower.controller

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.sse.Event
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.reactivex.Flowable
import io.seqera.tower.domain.Workflow
import io.seqera.tower.enums.SseErrorType
import io.seqera.tower.exchange.trace.sse.TraceSseResponse
import io.seqera.tower.service.ServerSentEventsService
import io.seqera.tower.service.UserService
import org.reactivestreams.Publisher

import javax.inject.Inject
import java.time.Duration

/**
 * Server Sent Events endpoints to receive live updates in the client.
 *
 */
@Controller("/sse")
@Secured(SecurityRule.IS_ANONYMOUS)
@Slf4j
class ServerSentEventsController {

    UserService userService

    ServerSentEventsService serverSentEventsService

    @Inject
    ServerSentEventsController(ServerSentEventsService serverSentEventsService) {
        this.userService = userService
        this.serverSentEventsService = serverSentEventsService
    }


    @Get("/workflow/{workflowId}")
    Publisher<Event<TraceSseResponse>> liveWorkflow(String workflowId) {
        log.info("Subscribing to live events of workflow: ${workflowId}")
        Flowable<Event<TraceSseResponse>> workflowFlowable
        try {
            workflowFlowable = serverSentEventsService.getOrCreateWorkflowPublisher(workflowId)
        }
        catch (Exception e) {
            String message = "Unexpected error while obtaining event emitter for workflow: ${workflowId}"
            log.error("${message} | ${e.message}", e)
            workflowFlowable = Flowable.just(Event.of(TraceSseResponse.ofError(SseErrorType.UNEXPECTED, message)))
        }

        return workflowFlowable
    }

    @Get("/user/{userId}")
    Publisher<Event<TraceSseResponse>> liveUser(Long userId) {
        log.info("Subscribing to live events of user: ${userId}")
        Flowable<Event> userFlowable
        try {
            userFlowable = serverSentEventsService.getOrCreateUserPublisher(userId)
        }
        catch (Exception e) {
            String message = "Unexpected error while obtaining event emitter for user: ${userId}"
            log.error("${message} | ${e.message}", e)

            return Flowable.just(Event.of(TraceSseResponse.ofError(SseErrorType.UNEXPECTED, message)))
        }

        return userFlowable
    }

}
