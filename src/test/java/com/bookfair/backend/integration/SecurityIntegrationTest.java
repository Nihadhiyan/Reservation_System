package com.bookfair.backend.integration;

import com.bookfair.backend.dto.organization.request.InviteRequest;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.*;
import com.bookfair.backend.repository.*;
import com.bookfair.backend.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class SecurityIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private OrganizationRepository organizationRepository;

        @Autowired
        private OrganizationMemberRepository memberRepository;

        @Autowired
        private OrganizationInviteRepository inviteRepository;

        @Autowired
        private JwtService jwtService;

        @Autowired
        private ObjectMapper objectMapper;

        private User superAdmin;
        private User customer;
        private Organization orgA;
        private Organization orgB;

        @BeforeEach
        void setUp() {
                // Create Super Admin
                superAdmin = new User();
                superAdmin.setUsername("superadmin");
                superAdmin.setEmail("admin@system.com");
                superAdmin.setPassword("password");
                superAdmin.setSystemRole(SystemRole.SUPER_ADMIN);
                superAdmin.setActive(true);
                superAdmin = userRepository.save(superAdmin);

                // Create Customer
                customer = new User();
                customer.setUsername("customer");
                customer.setEmail("customer@system.com");
                customer.setPassword("password");
                customer.setSystemRole(SystemRole.CUSTOMER);
                customer.setActive(true);
                customer = userRepository.save(customer);

                // Create Organizations
                orgA = new Organization();
                orgA.setName("Org A");
                orgA = organizationRepository.save(orgA);

                orgB = new Organization();
                orgB.setName("Org B");
                orgB = organizationRepository.save(orgB);
        }

        @Test
        void testCrossOrgMembershipAndJwtService() throws Exception {
                // 1. Cross-Org Membership Test
                // User is ORG_ADMIN in Org A and ORG_MEMBER in Org B
                OrganizationMember memberA = new OrganizationMember();
                memberA.setUser(customer);
                memberA.setOrganization(orgA);
                memberA.setRole(OrganizationRole.ORG_ADMIN);
                memberRepository.save(memberA);

                OrganizationMember memberB = new OrganizationMember();
                memberB.setUser(customer);
                memberB.setOrganization(orgB);
                memberB.setRole(OrganizationRole.ORG_MEMBER);
                memberRepository.save(memberB);

                // Verification Requirement: Ensure that the JwtService logic is verified
                String token = jwtService.generateAccessToken(customer);

                Map<String, String> extractedRoles = jwtService.extractOrgRoles(token);
                assertNotNull(extractedRoles, "Org roles map should be successfully extracted from JWT");
                assertEquals(OrganizationRole.ORG_ADMIN.name(), extractedRoles.get(orgA.getId().toString()),
                                "Should encode ORG_ADMIN for Org A");
                assertEquals(OrganizationRole.ORG_MEMBER.name(), extractedRoles.get(orgB.getId().toString()),
                                "Should encode ORG_MEMBER for Org B");

                // Call Org A endpoint (Should Succeed - because user is ORG_ADMIN)
                InviteRequest requestA = new InviteRequest();
                requestA.setOrgId(orgA.getId());
                requestA.setEmail("newuserA@system.com");
                requestA.setRole(OrganizationRole.ORG_MEMBER);

                mockMvc.perform(post("/api/organizations/invites")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestA)))
                                .andExpect(status().isOk());

                // Call Org B endpoint (Should Fail with 403 - because user is only ORG_MEMBER)
                InviteRequest requestB = new InviteRequest();
                requestB.setOrgId(orgB.getId());
                requestB.setEmail("newuserB@system.com");
                requestB.setRole(OrganizationRole.ORG_MEMBER);

                mockMvc.perform(post("/api/organizations/invites")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestB)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testInvitationLifecycle() throws Exception {
                // 2. The "Invitation Lifecycle" Test
                OrganizationMember memberA = new OrganizationMember();
                memberA.setUser(customer);
                memberA.setOrganization(orgA);
                memberA.setRole(OrganizationRole.ORG_ADMIN);
                memberRepository.save(memberA);

                String token = jwtService.generateAccessToken(customer);

                // Trigger invitation flow as ORG_ADMIN
                InviteRequest request = new InviteRequest();
                request.setOrgId(orgA.getId());
                request.setEmail("invitee@system.com");
                request.setRole(OrganizationRole.ORG_MEMBER);

                mockMvc.perform(post("/api/organizations/invites")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                // Verify that the OrganizationInvite token is created correctly
                assertFalse(inviteRepository.findAll().isEmpty(), "Invite should be saved to database");
                OrganizationInvite invite = inviteRepository.findAll().get(0);
                assertNotNull(invite.getToken(), "Invite token must be generated");
                assertEquals("invitee@system.com", invite.getEmail(), "Invitee email should match");
                assertFalse(invite.getUsed(), "Invite should not be marked as used yet");

                // Simulate a valid login for the invitee
                User invitee = new User();
                invitee.setUsername("invitee");
                invitee.setEmail("invitee@system.com");
                invitee.setPassword("password");
                invitee.setSystemRole(SystemRole.CUSTOMER);
                invitee.setActive(true);
                invitee = userRepository.save(invitee);

                String inviteeToken = jwtService.generateAccessToken(invitee);

                // Execute acceptInvite using the token
                mockMvc.perform(post("/api/organizations/invites/accept")
                                .header("Authorization", "Bearer " + inviteeToken)
                                .param("token", invite.getToken()))
                                .andExpect(status().isOk());

                // Verify the OrganizationMember mapping is persisted
                OrganizationMember newMember = memberRepository
                                .findByUserIdAndOrganizationId(invitee.getId(), orgA.getId())
                                .orElse(null);
                assertNotNull(newMember, "OrganizationMember mapping must be persisted");
                assertEquals(OrganizationRole.ORG_MEMBER, newMember.getRole(), "Assigned role must match invitation");

                // Verify the token is marked as used
                OrganizationInvite updatedInvite = inviteRepository.findById(invite.getId())
                                .orElseThrow(
                                                () -> new ResourceNotFoundException(inviteeToken,
                                                                ErrorCode.INVITE_NOT_FOUND));
                assertTrue(updatedInvite.getUsed(), "Invite must be marked as used");
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        void testSystemWidePermission_SuperAdmin() throws Exception {
                // 3. The "System-Wide Permission" Test - SUPER_ADMIN Access
                // Using @WithMockUser to simulate role easily for global endpoints using
                // GrantedAuthority
                mockMvc.perform(post("/api/v1/admin/system/maintenance"))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void testSystemWidePermission_Customer() throws Exception {
                // 3. The "System-Wide Permission" Test - CUSTOMER Access
                mockMvc.perform(post("/api/v1/admin/system/maintenance"))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testNullSafetyAndEdgeCases() throws Exception {
                // 4. The "Null-Safety & Edge Case" Test

                // Use Super Admin token so we bypass the controller-level permission check
                // and hit the Service layer where the "non-existent organization" validation
                // occurs.
                String superAdminToken = jwtService.generateAccessToken(superAdmin);

                // Submit a request to acceptInvite with a missing (null) token
                mockMvc.perform(post("/api/organizations/invites/accept")
                                .header("Authorization", "Bearer " + superAdminToken))
                                // Spring returns 400 Bad Request when a required @RequestParam is missing
                                .andExpect(status().isBadRequest());

                // Submit a request to createInvite with a non-existent organizationId
                InviteRequest request = new InviteRequest();
                request.setOrgId(UUID.randomUUID()); // Random UUID
                request.setEmail("test@system.com");
                request.setRole(OrganizationRole.ORG_MEMBER);

                mockMvc.perform(post("/api/organizations/invites")
                                .header("Authorization", "Bearer " + superAdminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                // GlobalExceptionHandler should map ResourceNotFoundException to 404
                                .andExpect(status().isNotFound());
        }
}
