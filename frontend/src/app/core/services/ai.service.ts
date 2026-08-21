import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AiServiceAdvisorRequest,
  AiServiceAdvisorResponse,
  AiServiceSummaryResponse
} from '../models/ai.models';

/**
 * AiService
 *
 * Calls backend AI Service Advisor and AI Service Summary endpoints.
 */
@Injectable({
  providedIn: 'root'
})
export class AiService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /**
   * Requests an AI service recommendation based on vehicle details and reported symptoms.
   */
  getServiceAdvice(request: AiServiceAdvisorRequest): Observable<AiServiceAdvisorResponse> {
    return this.http.post<AiServiceAdvisorResponse>(`${this.baseUrl}/ai/service-advisor`, request);
  }

  /**
   * Retrieves an AI-generated summary of a finalized historical service record.
   */
  getServiceSummary(recordId: number): Observable<AiServiceSummaryResponse> {
    return this.http.get<AiServiceSummaryResponse>(`${this.baseUrl}/service-records/${recordId}/ai-summary`);
  }
}
