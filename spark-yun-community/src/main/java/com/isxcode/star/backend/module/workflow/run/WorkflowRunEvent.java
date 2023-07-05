package com.isxcode.star.backend.module.workflow.run;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Builder
@Data
public class WorkflowRunEvent {

  private String flowInstanceId;

  private String workId;

  private String versionId;

  private List<List<String>> nodeMapping;

  private List<String> nodeList;

  private List<String> dagStartList;

  private List<String> dagEndList;

  private String userId;

  private String tenantId;

  public WorkflowRunEvent(String workId, WorkflowRunEvent workRunEvent) {

    this.workId = workId;
    this.flowInstanceId = workRunEvent.getFlowInstanceId();
    this.nodeMapping = workRunEvent.getNodeMapping();
    this.nodeList = workRunEvent.getNodeList();
    this.dagStartList = workRunEvent.getDagStartList();
    this.dagEndList = workRunEvent.getDagEndList();
    this.userId = workRunEvent.getUserId();
    this.tenantId = workRunEvent.getTenantId();
  }
}
